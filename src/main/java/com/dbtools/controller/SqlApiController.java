package com.dbtools.controller;

import com.dbtools.model.ApiResponse;
import com.dbtools.model.SqlRequest;
import com.dbtools.model.StorageResult;
import com.dbtools.service.SqlExecuteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql")
public class SqlApiController {

    private final SqlExecuteService sqlExecuteService;

    public SqlApiController(SqlExecuteService sqlExecuteService) {
        this.sqlExecuteService = sqlExecuteService;
    }

    @PostMapping("/execute")
    public ApiResponse<StorageResult> execute(@Valid @RequestBody SqlRequest request) {
        StorageResult result = sqlExecuteService.executeStorage(request.getDatabase(), request.getSql());
        if (result.isSuccess()) {
            return ApiResponse.ok(result);
        }
        return ApiResponse.fail(result.getMessage());
    }
}
