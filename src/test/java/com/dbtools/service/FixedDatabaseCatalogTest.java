package com.dbtools.service;

import com.dbtools.model.DatabaseConfig;
import com.dbtools.model.DatabaseView;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedDatabaseCatalogTest {

    private final FixedDatabaseCatalog catalog = new FixedDatabaseCatalog();

    @Test
    void exposesTwoPanweiDatabasesWithoutPasswords() {
        List<DatabaseView> views = catalog.listViews();
        assertEquals(2, views.size());
        assertEquals("app", views.get(0).getId());
        assertEquals("手机经分应用库", views.get(0).getLabel());
        assertEquals("config", views.get(1).getId());
        assertEquals("手机经分配置库", views.get(1).getLabel());
    }

    @Test
    void appDatabaseUsesGivenConnection() {
        FixedDatabaseCatalog.Entry entry = catalog.require(FixedDatabaseCatalog.APP_ID);
        DatabaseConfig config = toConfig(entry);
        assertEquals("dbsjjfapp", entry.username());
        assertEquals("Re3Bs#hP7nWd", entry.password());
        assertTrue(config.resolveJdbcUrl().startsWith(
                "jdbc:panweidb://c2zevendb1.panwei.db.amcc:17700,c2zevendb2.panwei.db.amcc:17700,c2zevendb3.panwei.db.amcc:17700/zevendbadm?"));
        assertTrue(config.resolveJdbcUrl().contains("currentSchema=dwmbbi"));
    }

    @Test
    void configDatabaseUsesGivenConnection() {
        FixedDatabaseCatalog.Entry entry = catalog.require(FixedDatabaseCatalog.CONFIG_ID);
        DatabaseConfig config = toConfig(entry);
        assertEquals("dbsjjfapp", entry.username());
        assertEquals("An5vM#Ya2RcF", entry.password());
        assertTrue(config.resolveJdbcUrl().startsWith(
                "jdbc:panweidb://zevendb1.panwei.db.amcc:17700,zevendb2.panwei.db.amcc:17700,zevendb3.panwei.db.amcc:17700/zevendb?"));
        assertTrue(config.resolveJdbcUrl().contains("targetServerType=master"));
        assertTrue(config.resolveJdbcUrl().contains("resultCaseMode=upper"));
    }

    @Test
    void rejectsUnknownDatabase() {
        assertThrows(IllegalArgumentException.class, () -> catalog.require("other"));
    }

    private static DatabaseConfig toConfig(FixedDatabaseCatalog.Entry entry) {
        DatabaseConfig config = new DatabaseConfig();
        config.setUrl(entry.url());
        return config;
    }
}
