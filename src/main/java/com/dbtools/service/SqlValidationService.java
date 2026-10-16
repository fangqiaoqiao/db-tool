package com.dbtools.service;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.UnsupportedStatement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.grant.Grant;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class SqlValidationService {

    private static final Pattern GRANT_TO_PATTERN = Pattern.compile("\\bTO\\b", Pattern.CASE_INSENSITIVE);

    private static final String DDL_ALLOWED_MESSAGE =
            "DDL页面仅允许执行 CREATE TABLE、ALTER TABLE、DROP TABLE、CREATE USER、CREATE ROLE、GRANT、COMMENT ON 语句";

    public enum SqlType {
        QUERY, UPDATE, DDL
    }

    public String normalizeSingleStatement(String sql) {
        List<String> statements = splitStatements(sql);
        if (statements.size() > 1) {
            throw new IllegalArgumentException("仅支持单条SQL语句执行，请勿包含多条语句");
        }
        return statements.get(0);
    }

    private static final Pattern ROUTINE_START = Pattern.compile(
            "CREATE\\s+(OR\\s+REPLACE\\s+)?(PROCEDURE|FUNCTION)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DOLLAR_TAG = Pattern.compile("\\$[A-Za-z0-9_]*\\$");

    public List<String> splitStatements(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }

        List<String> statements = new ArrayList<>();
        int len = sql.length();
        int i = 0;
        while (i < len) {
            i = skipTrivia(sql, i);
            if (i >= len) {
                break;
            }
            if (isRoutineStart(sql, i)) {
                int bodyEnd = findRoutineBodyEnd(sql, i);
                String statement = stripTrailingSlash(sql.substring(i, bodyEnd).trim());
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                i = skipRoutineTerminator(sql, bodyEnd);
                continue;
            }

            int end = readPlainStatementEnd(sql, i);
            String statement = sql.substring(i, end).trim();
            if (!statement.isEmpty() && !"/".equals(statement)) {
                statements.add(statement);
            }
            i = end;
            if (i < len && sql.charAt(i) == ';') {
                i++;
            }
        }

        if (statements.isEmpty()) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }
        return statements;
    }

    private boolean isRoutineStart(String sql, int start) {
        return ROUTINE_START.matcher(sql).region(start, sql.length()).lookingAt();
    }

    private int findRoutineBodyEnd(String sql, int start) {
        int len = sql.length();
        int i = start;
        boolean sawDollarBody = false;
        while (i < len) {
            char c = sql.charAt(i);
            if (c == '\'') {
                i = skipQuotedString(sql, i, '\'');
                continue;
            }
            if (c == '"') {
                i = skipQuotedString(sql, i, '"');
                continue;
            }
            if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
                i = skipLineComment(sql, i);
                continue;
            }
            if (c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                i = skipBlockComment(sql, i);
                continue;
            }
            if (c == '$') {
                String tag = readDollarTag(sql, i);
                if (tag != null) {
                    int close = sql.indexOf(tag, i + tag.length());
                    if (close >= 0) {
                        i = close + tag.length();
                        sawDollarBody = true;
                        continue;
                    }
                }
            }
            // 非 dollar-quoted 过程体依赖独立一行 /；dollar-quoted 函数在 $$ 后可能还有 LANGUAGE 等子句，以 ; 结束
            if (c == '/' && isSlashTerminator(sql, i)) {
                return i;
            }
            if (c == ';' && sawDollarBody) {
                return i + 1;
            }
            i++;
        }
        return len;
    }

    private int skipTrivia(String sql, int index) {
        int i = index;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                i = skipLineComment(sql, i);
                continue;
            }
            if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                i = skipBlockComment(sql, i);
                continue;
            }
            break;
        }
        return i;
    }

    private int skipRoutineTerminator(String sql, int index) {
        int i = skipWhitespace(sql, index);
        if (i < sql.length() && sql.charAt(i) == '/' && isSlashTerminator(sql, i)) {
            i++;
        }
        i = skipWhitespace(sql, i);
        if (i < sql.length() && sql.charAt(i) == ';') {
            i++;
        }
        return i;
    }

    private int readPlainStatementEnd(String sql, int start) {
        int len = sql.length();
        int i = start;
        while (i < len) {
            char c = sql.charAt(i);
            if (c == '\'') {
                i = skipQuotedString(sql, i, '\'');
                continue;
            }
            if (c == '"') {
                i = skipQuotedString(sql, i, '"');
                continue;
            }
            if (c == '-' && i + 1 < len && sql.charAt(i + 1) == '-') {
                i = skipLineComment(sql, i);
                continue;
            }
            if (c == '/' && i + 1 < len && sql.charAt(i + 1) == '*') {
                i = skipBlockComment(sql, i);
                continue;
            }
            if (c == '$') {
                String tag = readDollarTag(sql, i);
                if (tag != null) {
                    int close = sql.indexOf(tag, i + tag.length());
                    if (close >= 0) {
                        i = close + tag.length();
                        continue;
                    }
                }
            }
            if (c == ';') {
                return i;
            }
            i++;
        }
        return len;
    }

    private boolean isSlashTerminator(String sql, int index) {
        int lineStart = index == 0 ? 0 : sql.lastIndexOf('\n', index - 1) + 1;
        for (int j = lineStart; j < index; j++) {
            if (!Character.isWhitespace(sql.charAt(j))) {
                return false;
            }
        }
        int k = index + 1;
        while (k < sql.length()) {
            char c = sql.charAt(k);
            if (c == '\n' || c == '\r') {
                break;
            }
            if (!Character.isWhitespace(c)) {
                return false;
            }
            k++;
        }
        return true;
    }

    private String readDollarTag(String sql, int start) {
        var matcher = DOLLAR_TAG.matcher(sql);
        if (matcher.region(start, sql.length()).lookingAt()) {
            return matcher.group();
        }
        return null;
    }

    private int skipWhitespace(String sql, int index) {
        int i = index;
        while (i < sql.length() && Character.isWhitespace(sql.charAt(i))) {
            i++;
        }
        return i;
    }

    private int skipLineComment(String sql, int index) {
        int newline = sql.indexOf('\n', index);
        return newline < 0 ? sql.length() : newline + 1;
    }

    private int skipBlockComment(String sql, int index) {
        int close = sql.indexOf("*/", index + 2);
        return close < 0 ? sql.length() : close + 2;
    }

    private String stripTrailingSlash(String sql) {
        String s = sql.stripTrailing();
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1).stripTrailing();
        }
        return s;
    }

    public void validate(String sql, SqlType type) {
        switch (type) {
            case QUERY -> {
                String normalized = normalizeSingleStatement(sql);
                validateQuery(parseOrThrow(normalized));
            }
            case UPDATE -> {
                for (String statement : splitStatements(sql)) {
                    validateUpdate(parseOrThrow(statement), statement);
                }
            }
            case DDL -> {
                for (String statement : splitStatements(sql)) {
                    validateDdl(statement);
                }
            }
            default -> throw new IllegalArgumentException("不支持的SQL类型");
        }
    }

    public Statement parseOrThrow(String normalized) {
        try {
            return CCJSqlParserUtil.parse(normalized);
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("SQL语法解析失败: " + e.getMessage());
        }
    }

    private void validateQuery(Statement statement) {
        if (!(statement instanceof Select)) {
            throw new IllegalArgumentException("查询页面仅允许执行 SELECT 语句");
        }
    }

    private void validateUpdate(Statement statement, String normalized) {
        if (statement instanceof Insert) {
            return;
        }
        if (statement instanceof Update update) {
            if (update.getWhere() == null) {
                throw new IllegalArgumentException("UPDATE 语句必须包含 WHERE 条件");
            }
            return;
        }
        if (statement instanceof Delete delete) {
            if (delete.getWhere() == null) {
                throw new IllegalArgumentException("DELETE 语句必须包含 WHERE 条件");
            }
            return;
        }
        throw new IllegalArgumentException("更新页面仅允许执行 INSERT、UPDATE、DELETE 语句");
    }

    private void validateDdl(String normalized) {
        try {
            validateParsedDdl(CCJSqlParserUtil.parse(normalized), normalized);
        } catch (JSQLParserException e) {
            if (isAllowedPrivilegeDdl(normalized) || isAllowedCommentDdl(normalized)) {
                return;
            }
            throw new IllegalArgumentException("SQL语法解析失败: " + e.getMessage());
        }
    }

    private void validateParsedDdl(Statement statement, String normalized) {
        // COMMENT ON / CREATE USER 等可能被解析为多种 Statement 类型，优先按原文放行
        if (isAllowedPrivilegeDdl(normalized) || isAllowedCommentDdl(normalized)) {
            return;
        }
        if (statement instanceof CreateTable
                || statement instanceof Alter
                || statement instanceof Drop
                || statement instanceof Grant) {
            return;
        }
        throw new IllegalArgumentException(DDL_ALLOWED_MESSAGE);
    }

    private boolean isAllowedCommentDdl(String sql) {
        return sql.trim().toUpperCase(Locale.ROOT).startsWith("COMMENT ON ");
    }

    private boolean isAllowedPrivilegeDdl(String sql) {
        String upper = sql.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("CREATE USER ") || upper.startsWith("CREATE ROLE ")) {
            return true;
        }
        return upper.startsWith("GRANT ") && GRANT_TO_PATTERN.matcher(sql).find();
    }

    public String getUpdateActionLabel(Statement statement) {
        if (statement instanceof Insert) {
            return "插入";
        }
        if (statement instanceof Update) {
            return "更新";
        }
        if (statement instanceof Delete) {
            return "删除";
        }
        return "影响";
    }

    public Statement parseStatement(String sql) {
        return parseOrThrow(normalizeSingleStatement(sql));
    }

    private int skipQuotedString(String sql, int start, char quote) {
        int i = start + 1;
        int len = sql.length();
        while (i < len) {
            char c = sql.charAt(i);
            if (c == quote) {
                if (i + 1 < len && sql.charAt(i + 1) == quote) {
                    i += 2;
                    continue;
                }
                return i + 1;
            }
            i++;
        }
        return len;
    }
}
