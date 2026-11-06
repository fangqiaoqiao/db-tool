package com.dbtools.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户配置的一条数据库连接（对应配置页中的一个区域）。
 */
public class UserDatabaseEntry {

    @NotBlank(message = "区域名称不能为空")
    private String regionName;

    @NotBlank(message = "数据库名称不能为空")
    private String name;

    @NotBlank(message = "数据库 URL 不能为空")
    private String url;

    @NotBlank(message = "数据库账号不能为空")
    private String username;

    private String password = "";

    public UserDatabaseEntry() {
    }

    public UserDatabaseEntry(String regionName, String name, String url, String username, String password) {
        this.regionName = regionName;
        this.name = name;
        this.url = url;
        this.username = username;
        this.password = password == null ? "" : password;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        this.password = password == null ? "" : password;
    }

    /**
     * 从区域名称（如“数据库12”）解析序号；无法解析时返回 Integer.MAX_VALUE 以便排到末尾。
     */
    public int regionOrder() {
        if (regionName == null) {
            return Integer.MAX_VALUE;
        }
        String digits = regionName.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
