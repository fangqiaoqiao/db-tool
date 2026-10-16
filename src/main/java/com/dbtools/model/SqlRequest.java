package com.dbtools.model;

import jakarta.validation.constraints.NotBlank;

public class SqlRequest {

    @NotBlank(message = "请选择数据库")
    private String database;

    @NotBlank(message = "SQL语句不能为空")
    private String sql;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
