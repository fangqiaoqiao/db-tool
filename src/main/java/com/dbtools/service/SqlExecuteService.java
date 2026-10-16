package com.dbtools.service;

import com.dbtools.model.ScriptBatchItem;
import com.dbtools.model.StorageResult;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class SqlExecuteService {

    private final DatabaseConfigService configService;
    private final SqlValidationService validationService;

    public SqlExecuteService(DatabaseConfigService configService,
                             SqlValidationService validationService) {
        this.configService = configService;
        this.validationService = validationService;
    }

    public StorageResult executeStorage(String databaseId, String sql) {
        try {
            List<String> statements = validationService.splitStatements(sql);

            try (Connection conn = configService.getConnection(databaseId)) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    List<ScriptBatchItem> items = new ArrayList<>();
                    for (String statement : statements) {
                        collectResults(stmt, statement, items);
                    }
                    conn.commit();
                    return buildResult(statements.size(), items);
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (IllegalArgumentException e) {
            return StorageResult.error(e.getMessage());
        } catch (SQLException e) {
            return StorageResult.error("执行失败: " + e.getMessage());
        }
    }

    private void collectResults(Statement stmt, String statement, List<ScriptBatchItem> items) throws SQLException {
        boolean hasResultSet = stmt.execute(statement);
        int updateCount = stmt.getUpdateCount();
        boolean produced = false;
        while (true) {
            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    List<String> columns = readColumns(rs);
                    items.add(ScriptBatchItem.query(columns, readRows(rs, columns.size())));
                }
                produced = true;
            } else if (updateCount >= 0) {
                items.add(ScriptBatchItem.update(updateCount));
                produced = true;
            } else {
                break;
            }
            hasResultSet = stmt.getMoreResults();
            updateCount = stmt.getUpdateCount();
        }
        if (!produced) {
            items.add(ScriptBatchItem.update(0));
        }
    }

    private StorageResult buildResult(int statementCount, List<ScriptBatchItem> items) {
        List<String> lastColumns = null;
        List<List<Object>> lastRows = null;
        int affectedRows = 0;
        int updateCount = 0;
        int resultSetCount = 0;
        for (ScriptBatchItem item : items) {
            if (item.isHasResultSet()) {
                resultSetCount++;
                lastColumns = item.getColumns();
                lastRows = item.getRows();
            } else {
                updateCount++;
                affectedRows += item.getAffectedRows();
            }
        }

        StorageResult result;
        if (lastColumns != null) {
            String extra = updateCount > 0 ? "另影响 " + affectedRows + " 条数据" : null;
            result = StorageResult.querySuccess(lastColumns, lastRows, extra);
        } else {
            result = StorageResult.executeSuccess(affectedRows, statementCount);
        }
        if (statementCount > 1 || resultSetCount > 1) {
            result.setMessage(buildBatchMessage(statementCount, resultSetCount, affectedRows, updateCount));
        }
        result.setStatementCount(statementCount);
        result.setItems(items);
        return result;
    }

    private String buildBatchMessage(int statementCount, int resultSetCount, int affectedRows, int updateCount) {
        StringBuilder message = new StringBuilder("成功执行 ").append(statementCount).append(" 条语句");
        if (resultSetCount > 0) {
            message.append("，返回 ").append(resultSetCount).append(" 个结果集");
        }
        if (updateCount > 0) {
            message.append("，共影响 ").append(affectedRows).append(" 条数据");
        }
        return message.toString();
    }

    private List<String> readColumns(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(meta.getColumnLabel(i));
        }
        return columns;
    }

    private List<List<Object>> readRows(ResultSet rs, int columnCount) throws SQLException {
        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}
