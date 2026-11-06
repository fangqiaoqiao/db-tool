package com.dbtools.service;

import com.dbtools.model.DatabaseConfig;
import com.dbtools.model.DatabaseView;
import com.dbtools.model.UserDatabaseEntry;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

@Service
public class DatabaseConfigService {

    private final UserDatabaseCatalog catalog;

    public DatabaseConfigService(UserDatabaseCatalog catalog) {
        this.catalog = catalog;
    }

    public List<DatabaseView> listDatabases() {
        return catalog.listViews();
    }

    public List<UserDatabaseEntry> listEntries() {
        return catalog.listEntries();
    }

    public void saveEntries(List<UserDatabaseEntry> entries) {
        catalog.saveAll(entries);
    }

    public Connection getConnection(String databaseId) throws SQLException {
        UserDatabaseEntry entry = catalog.require(databaseId);
        return openConnection(entry.getUrl(), entry.getUsername(), entry.getPassword());
    }

    /**
     * 使用指定连接信息测试连通性。
     */
    public String testConnection(String url, String username, String password) {
        try (Connection ignored = openConnection(url, username, password)) {
            return "连接成功";
        } catch (SQLException e) {
            return "连接失败: " + e.getMessage();
        }
    }

    public String testConnectionById(String databaseId) {
        UserDatabaseEntry entry = catalog.require(databaseId);
        try {
            String message = testConnection(entry.getUrl(), entry.getUsername(), entry.getPassword());
            boolean ok = message.startsWith("连接成功");
            if (ok) {
                return "切换成功，" + message + "（" + entry.getName() + "）";
            }
            return "切换失败，" + message;
        } catch (IllegalArgumentException e) {
            return "切换失败，" + e.getMessage();
        }
    }

    private Connection openConnection(String url, String username, String password) throws SQLException {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setDriverClass(DatabaseConfig.DEFAULT_DRIVER_CLASS);
        dbConfig.setUrl(url);
        dbConfig.setUsername(username);
        dbConfig.setPassword(password == null ? "" : password);
        loadDriver(dbConfig.resolveDriverClass());
        return DriverManager.getConnection(
                dbConfig.resolveJdbcUrl(),
                dbConfig.getUsername(),
                dbConfig.getPassword()
        );
    }

    private static void loadDriver(String driverClass) throws SQLException {
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new SQLException("未找到数据库驱动: " + driverClass, e);
        }
    }
}
