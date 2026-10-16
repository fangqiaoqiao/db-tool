package com.dbtools.model;

import jakarta.validation.constraints.NotBlank;

public class DatabaseConfig {

    public static final String DEFAULT_DRIVER_CLASS = "org.panweidb.Driver";

    @NotBlank(message = "驱动类不能为空")
    private String driverClass = DEFAULT_DRIVER_CLASS;

    @NotBlank(message = "连接 URL 不能为空")
    private String url = "jdbc:panweidb://localhost:17700/mydb?currentSchema=public&targetServerType=master&resultCaseMode=upper";

    @NotBlank(message = "账号不能为空")
    private String username = "";

    private String password = "";

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String resolveDriverClass() {
        String trimmed = driverClass == null ? "" : driverClass.trim();
        return trimmed.isEmpty() ? DEFAULT_DRIVER_CLASS : trimmed;
    }

    public String resolveJdbcUrl() {
        String trimmed = url.trim();
        if (trimmed.startsWith("panweidb://")) {
            return "jdbc:" + trimmed;
        }
        if (!trimmed.startsWith("jdbc:")) {
            return "jdbc:panweidb://" + trimmed;
        }
        return trimmed;
    }
}
