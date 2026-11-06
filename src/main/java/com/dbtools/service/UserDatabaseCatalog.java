package com.dbtools.service;

import com.dbtools.model.DatabaseView;
import com.dbtools.model.UserDatabaseEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 用户数据库配置目录。数据写入容器内临时目录文件。
 * 进程启动时清空旧文件，从而在容器/进程重启后配置丢失；
 * 同一进程生命周期内保存后可长期保留。
 */
@Component
public class UserDatabaseCatalog {

    private final Path configFile;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public UserDatabaseCatalog(
            @Value("${dbtools.config-file:}") String configFileOverride) {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        if (configFileOverride != null && !configFileOverride.isBlank()) {
            this.configFile = Path.of(configFileOverride.trim());
        } else {
            this.configFile = Path.of(System.getProperty("java.io.tmpdir"), "db-tools", "db-config.json");
        }
    }

    @PostConstruct
    public void clearStaleConfigOnStartup() {
        lock.writeLock().lock();
        try {
            Files.deleteIfExists(configFile);
        } catch (IOException e) {
            throw new IllegalStateException("启动时清理数据库配置失败: " + e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<UserDatabaseEntry> listEntries() {
        lock.readLock().lock();
        try {
            return sortedCopy(readUnlocked());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<DatabaseView> listViews() {
        return listEntries().stream()
                .map(entry -> new DatabaseView(entry.getRegionName(), entry.getName()))
                .toList();
    }

    public UserDatabaseEntry require(String databaseId) {
        if (databaseId == null || databaseId.isBlank()) {
            throw new IllegalArgumentException("请选择数据库");
        }
        String id = databaseId.trim();
        return listEntries().stream()
                .filter(entry -> id.equals(entry.getRegionName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知的数据库"));
    }

    public void saveAll(List<UserDatabaseEntry> entries) {
        List<UserDatabaseEntry> normalized = normalize(entries);
        lock.writeLock().lock();
        try {
            writeUnlocked(normalized);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Path getConfigFile() {
        return configFile;
    }

    private List<UserDatabaseEntry> readUnlocked() {
        if (!Files.exists(configFile)) {
            return List.of();
        }
        try {
            byte[] bytes = Files.readAllBytes(configFile);
            if (bytes.length == 0) {
                return List.of();
            }
            List<UserDatabaseEntry> loaded = objectMapper.readValue(bytes, new TypeReference<>() {
            });
            return loaded == null ? List.of() : loaded;
        } catch (IOException e) {
            throw new IllegalStateException("读取数据库配置失败: " + e.getMessage(), e);
        }
    }

    private void writeUnlocked(List<UserDatabaseEntry> entries) {
        try {
            Path parent = configFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tempFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");
            objectMapper.writeValue(tempFile.toFile(), entries);
            try {
                Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("保存数据库配置失败: " + e.getMessage(), e);
        }
    }

    private static List<UserDatabaseEntry> normalize(List<UserDatabaseEntry> entries) {
        if (entries == null) {
            return List.of();
        }
        List<UserDatabaseEntry> result = new ArrayList<>();
        for (UserDatabaseEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            String regionName = trimToEmpty(entry.getRegionName());
            String name = trimToEmpty(entry.getName());
            String url = trimToEmpty(entry.getUrl());
            String username = trimToEmpty(entry.getUsername());
            if (regionName.isEmpty()) {
                throw new IllegalArgumentException("区域名称不能为空");
            }
            if (name.isEmpty()) {
                throw new IllegalArgumentException(regionName + "：数据库名称不能为空");
            }
            if (url.isEmpty()) {
                throw new IllegalArgumentException(regionName + "：数据库 URL 不能为空");
            }
            if (username.isEmpty()) {
                throw new IllegalArgumentException(regionName + "：数据库账号不能为空");
            }
            result.add(new UserDatabaseEntry(
                    regionName,
                    name,
                    url,
                    username,
                    entry.getPassword() == null ? "" : entry.getPassword()
            ));
        }
        long distinctRegions = result.stream().map(UserDatabaseEntry::getRegionName).distinct().count();
        if (distinctRegions != result.size()) {
            throw new IllegalArgumentException("区域名称不能重复");
        }
        return result;
    }

    private static List<UserDatabaseEntry> sortedCopy(List<UserDatabaseEntry> entries) {
        List<UserDatabaseEntry> copy = new ArrayList<>(entries);
        copy.sort(Comparator.comparingInt(UserDatabaseEntry::regionOrder)
                .thenComparing(UserDatabaseEntry::getRegionName, Comparator.nullsLast(String::compareTo)));
        return copy;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
