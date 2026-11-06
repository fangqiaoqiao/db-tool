package com.dbtools.controller;

import com.dbtools.model.ApiResponse;
import com.dbtools.model.DatabaseView;
import com.dbtools.model.UserDatabaseEntry;
import com.dbtools.service.DatabaseConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/database")
public class DatabaseApiController {

    private final DatabaseConfigService configService;

    public DatabaseApiController(DatabaseConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/list")
    public ApiResponse<List<DatabaseView>> list() {
        return ApiResponse.ok(configService.listDatabases());
    }

    @GetMapping("/configs")
    public ApiResponse<List<UserDatabaseEntry>> configs() {
        return ApiResponse.ok(configService.listEntries());
    }

    @PutMapping("/configs")
    public ApiResponse<List<DatabaseView>> save(@RequestBody List<UserDatabaseEntry> entries) {
        try {
            configService.saveEntries(entries == null ? List.of() : entries);
            return ApiResponse.ok("保存成功", configService.listDatabases());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/test")
    public ApiResponse<Map<String, Object>> test(@RequestParam("id") String id) {
        String message = configService.testConnectionById(id);
        boolean success = message.startsWith("切换成功");
        if (success) {
            return ApiResponse.ok(message, Map.of("ok", true, "message", message));
        }
        return ApiResponse.fail(message);
    }
}
