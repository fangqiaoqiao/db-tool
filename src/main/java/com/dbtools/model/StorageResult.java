package com.dbtools.model;

import java.util.List;

public class StorageResult {

    private boolean success;
    private String message;
    private boolean hasResultSet;
    private List<String> columns;
    private List<List<Object>> rows;
    private int rowCount;
    private int affectedRows;
    private int statementCount;
    private List<ScriptBatchItem> items;

    public static StorageResult querySuccess(List<String> columns, List<List<Object>> rows, String extraMessage) {
        StorageResult result = new StorageResult();
        result.success = true;
        result.hasResultSet = true;
        result.columns = columns;
        result.rows = rows;
        result.rowCount = rows != null ? rows.size() : 0;
        result.message = "执行成功，返回 " + result.rowCount + " 条记录"
                + (extraMessage != null && !extraMessage.isBlank() ? "；" + extraMessage : "");
        return result;
    }

    public static StorageResult executeSuccess(int affectedRows, int statementCount) {
        StorageResult result = new StorageResult();
        result.success = true;
        result.hasResultSet = false;
        result.affectedRows = affectedRows;
        if (statementCount <= 1) {
            result.message = "执行成功，影响 " + affectedRows + " 条数据";
        } else {
            result.message = "成功执行 " + statementCount + " 条语句，共影响 " + affectedRows + " 条数据";
        }
        return result;
    }

    public static StorageResult error(String message) {
        StorageResult result = new StorageResult();
        result.success = false;
        result.message = message;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isHasResultSet() {
        return hasResultSet;
    }

    public void setHasResultSet(boolean hasResultSet) {
        this.hasResultSet = hasResultSet;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<List<Object>> getRows() {
        return rows;
    }

    public void setRows(List<List<Object>> rows) {
        this.rows = rows;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(int affectedRows) {
        this.affectedRows = affectedRows;
    }

    public int getStatementCount() {
        return statementCount;
    }

    public void setStatementCount(int statementCount) {
        this.statementCount = statementCount;
    }

    public List<ScriptBatchItem> getItems() {
        return items;
    }

    public void setItems(List<ScriptBatchItem> items) {
        this.items = items;
    }
}
