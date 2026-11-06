package com.dbtools.service;

import com.dbtools.model.DatabaseView;
import com.dbtools.model.UserDatabaseEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDatabaseCatalogTest {

    @TempDir
    Path tempDir;

    @Test
    void startsEmptyAndPersistsSortedByRegionOrder() {
        Path file = tempDir.resolve("db-config.json");
        UserDatabaseCatalog catalog = new UserDatabaseCatalog(file.toString());

        assertTrue(catalog.listViews().isEmpty());

        catalog.saveAll(List.of(
                new UserDatabaseEntry("数据库5", "库五", "panweidb://host/db5", "u5", "p5"),
                new UserDatabaseEntry("数据库1", "库一", "panweidb://host/db1", "u1", "p1"),
                new UserDatabaseEntry("数据库2", "库二", "panweidb://host/db2", "u2", "p2")
        ));

        List<DatabaseView> views = catalog.listViews();
        assertEquals(3, views.size());
        assertEquals("数据库1", views.get(0).getId());
        assertEquals("库一", views.get(0).getLabel());
        assertEquals("数据库2", views.get(1).getId());
        assertEquals("库二", views.get(1).getLabel());
        assertEquals("数据库5", views.get(2).getId());
        assertEquals("库五", views.get(2).getLabel());

        UserDatabaseCatalog reloaded = new UserDatabaseCatalog(file.toString());
        assertEquals(3, reloaded.listEntries().size());
        assertEquals("p1", reloaded.require("数据库1").getPassword());
    }

    @Test
    void deleteDoesNotRenumberRemainingRegions() {
        Path file = tempDir.resolve("db-config.json");
        UserDatabaseCatalog catalog = new UserDatabaseCatalog(file.toString());
        catalog.saveAll(List.of(
                new UserDatabaseEntry("数据库1", "A", "url1", "u", "p"),
                new UserDatabaseEntry("数据库2", "B", "url2", "u", "p"),
                new UserDatabaseEntry("数据库3", "C", "url3", "u", "p")
        ));
        catalog.saveAll(List.of(
                new UserDatabaseEntry("数据库1", "A", "url1", "u", "p"),
                new UserDatabaseEntry("数据库3", "C", "url3", "u", "p")
        ));

        List<DatabaseView> views = catalog.listViews();
        assertEquals(2, views.size());
        assertEquals("数据库1", views.get(0).getId());
        assertEquals("数据库3", views.get(1).getId());
    }

    @Test
    void allowsSavingEmptyList() {
        Path file = tempDir.resolve("db-config.json");
        UserDatabaseCatalog catalog = new UserDatabaseCatalog(file.toString());
        catalog.saveAll(List.of(
                new UserDatabaseEntry("数据库1", "A", "url1", "u", "p")
        ));
        catalog.saveAll(List.of());
        assertTrue(catalog.listViews().isEmpty());
    }

    @Test
    void rejectsUnknownDatabase() {
        Path file = tempDir.resolve("db-config.json");
        UserDatabaseCatalog catalog = new UserDatabaseCatalog(file.toString());
        assertThrows(IllegalArgumentException.class, () -> catalog.require("other"));
    }

    @Test
    void rejectsBlankName() {
        Path file = tempDir.resolve("db-config.json");
        UserDatabaseCatalog catalog = new UserDatabaseCatalog(file.toString());
        assertThrows(IllegalArgumentException.class, () -> catalog.saveAll(List.of(
                new UserDatabaseEntry("数据库1", "  ", "url", "u", "p")
        )));
    }

    @Test
    void startupClearsPreviousConfigFile() throws Exception {
        Path file = tempDir.resolve("db-config.json");
        UserDatabaseCatalog first = new UserDatabaseCatalog(file.toString());
        first.saveAll(List.of(
                new UserDatabaseEntry("数据库1", "A", "url1", "u", "p")
        ));
        assertEquals(1, first.listEntries().size());

        UserDatabaseCatalog restarted = new UserDatabaseCatalog(file.toString());
        restarted.clearStaleConfigOnStartup();
        assertTrue(restarted.listViews().isEmpty());
    }
}
