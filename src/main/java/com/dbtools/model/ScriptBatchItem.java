package com.dbtools.model;

import java.util.List;

public class ScriptBatchItem {

    private boolean hasResultSet;
    private List<String> columns;
    private List<List<Object>> rows;
    private int rowCount;
    private int affectedRows;

    public static ScriptBatchItem query(List<String> columns, List<List<Object>> rows) {
        ScriptBatchItem item = new ScriptBatchItem();
        item.hasResultSet = true;
        item.columns = columns;
        item.rows = rows;
        item.rowCount = rows == null ? 0 : rows.size();
        return item;
    }

    public static ScriptBatchItem update(int affectedRows) {
        ScriptBatchItem item = new ScriptBatchItem();
        item.hasResultSet = false;
        item.affectedRows = Math.max(affectedRows, 0);
        return item;
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
}
