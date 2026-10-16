package com.dbtools.service;

import com.dbtools.model.DatabaseView;

import java.util.List;

public class FixedDatabaseCatalog {

    public record Entry(String id, String label, String url, String username, String password) {
    }

    public static final String APP_ID = "app";
    public static final String CONFIG_ID = "config";

    private static final List<Entry> ENTRIES = List.of(
            new Entry(
                    APP_ID,
                    "手机经分应用库",
                    "panweidb://c2zevendb1.panwei.db.amcc:17700,c2zevendb2.panwei.db.amcc:17700,c2zevendb3.panwei.db.amcc:17700/zevendbadm?currentSchema=dwmbbi&targetServerType=master&resultCaseMode=upper",
                    "dbsjjfapp",
                    "Re3Bs#hP7nWd"),
            new Entry(
                    CONFIG_ID,
                    "手机经分配置库",
                    "panweidb://zevendb1.panwei.db.amcc:17700,zevendb2.panwei.db.amcc:17700,zevendb3.panwei.db.amcc:17700/zevendb?currentSchema=dwmbbi&targetServerType=master&resultCaseMode=upper",
                    "dbsjjfapp",
                    "An5vM#Ya2RcF")
    );

    public List<DatabaseView> listViews() {
        return ENTRIES.stream()
                .map(entry -> new DatabaseView(entry.id(), entry.label()))
                .toList();
    }

    public Entry require(String databaseId) {
        if (databaseId == null || databaseId.isBlank()) {
            throw new IllegalArgumentException("请选择数据库");
        }
        return ENTRIES.stream()
                .filter(entry -> entry.id().equals(databaseId.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的数据库"));
    }
}
