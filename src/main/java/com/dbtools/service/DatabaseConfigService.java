package com.dbtools.service;

import com.dbtools.model.DatabaseConfig;
import com.dbtools.model.DatabaseView;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

@Service
public class DatabaseConfigService {

    private final FixedDatabaseCatalog catalog = new FixedDatabaseCatalog();

    public List<DatabaseView> listDatabases() {
        return catalog.listViews();
    }

    public Connection getConnection(String databaseId) throws SQLException {
        FixedDatabaseCatalog.Entry entry = catalog.require(databaseId);
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setDriverClass(DatabaseConfig.DEFAULT_DRIVER_CLASS);
        dbConfig.setUrl(entry.url());
        dbConfig.setUsername(entry.username());
        dbConfig.setPassword(entry.password());
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
