package com.dbtools.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlValidationServiceTest {

    private SqlValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new SqlValidationService();
    }

    @Test
    void allowsCreateUserAndRole() {
        assertDoesNotThrow(() -> validationService.validate(
                "CREATE USER app_user WITH PASSWORD 'secret'", SqlValidationService.SqlType.DDL));
        assertDoesNotThrow(() -> validationService.validate(
                "CREATE ROLE app_user LOGIN PASSWORD 'secret'", SqlValidationService.SqlType.DDL));
    }

    @Test
    void allowsGrantStatements() {
        assertDoesNotThrow(() -> validationService.validate(
                "GRANT SELECT ON users TO app_user", SqlValidationService.SqlType.DDL));
        assertDoesNotThrow(() -> validationService.validate(
                "GRANT ALL PRIVILEGES ON DATABASE mydb TO app_user", SqlValidationService.SqlType.DDL));
        assertDoesNotThrow(() -> validationService.validate(
                "GRANT INSERT, UPDATE ON TABLE users TO app_user", SqlValidationService.SqlType.DDL));
    }

    @Test
    void rejectsUnsupportedDdl() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validationService.validate("CREATE INDEX idx ON users (id)", SqlValidationService.SqlType.DDL));
        assertTrue(ex.getMessage().contains("DDL页面仅允许执行"));
    }

    @Test
    void allowsCommentOnColumnDdl() {
        assertDoesNotThrow(() -> validationService.validate(
                "COMMENT ON COLUMN users.name IS '用户名'", SqlValidationService.SqlType.DDL));
    }

    @Test
    void allowsMultiStatementDdl() {
        String sql = """
                CREATE TABLE demo (id int, name varchar(50));
                COMMENT ON COLUMN demo.name IS '名称';
                """;
        assertDoesNotThrow(() -> validationService.validate(sql, SqlValidationService.SqlType.DDL));
    }

    @Test
    void allowsMultiStatementInsert() {
        String sql = """
                INSERT INTO users (id, name) VALUES (1, 'a');
                INSERT INTO users (id, name) VALUES (2, 'b');
                """;
        assertDoesNotThrow(() -> validationService.validate(sql, SqlValidationService.SqlType.UPDATE));
    }

    @Test
    void splitStatementsIgnoresSemicolonInStringLiteral() {
        List<String> statements = validationService.splitStatements(
                "INSERT INTO users (name) VALUES ('a;b'); INSERT INTO users (name) VALUES ('c')");
        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("'a;b'"));
    }

    @Test
    void splitStatementsKeepsCreateProcedureAsOneStatement() {
        String sql = """
                CREATE OR REPLACE PROCEDURE dwmbbi.p_demo(curDay IN VARCHAR2)
                AS
                    sql_str VARCHAR2(32767);
                    step_no number;
                begin
                    sql_str := 'select 1; select case when 1=1 then 1 else 0 end';
                    execute sql_str;
                END;
                /
                """;
        List<String> statements = validationService.splitStatements(sql);
        assertEquals(1, statements.size());
        assertTrue(statements.get(0).startsWith("CREATE OR REPLACE PROCEDURE"));
        assertTrue(statements.get(0).contains("sql_str VARCHAR2(32767);"));
        assertTrue(statements.get(0).trim().endsWith("END;"));
        assertTrue(!statements.get(0).trim().endsWith("/"));
    }

    @Test
    void splitStatementsKeepsProcedureAndFollowingSql() {
        String sql = """
                CREATE OR REPLACE PROCEDURE dwmbbi.p_demo
                AS
                begin
                    null;
                END;
                /
                SELECT 1;
                INSERT INTO t (id) VALUES (1);
                """;
        List<String> statements = validationService.splitStatements(sql);
        assertEquals(3, statements.size());
        assertTrue(statements.get(0).startsWith("CREATE OR REPLACE PROCEDURE"));
        assertTrue(statements.get(0).trim().endsWith("END;"));
        assertEquals("SELECT 1", statements.get(1));
        assertTrue(statements.get(2).startsWith("INSERT INTO t"));
    }

    @Test
    void splitStatementsKeepsDollarQuotedFunctionAndFollowingSql() {
        String sql = """
                CREATE FUNCTION demo_fn() RETURNS int AS $$
                BEGIN
                    RETURN 1;
                END;
                $$;
                SELECT 1;
                """;
        List<String> statements = validationService.splitStatements(sql);
        assertEquals(2, statements.size());
        assertTrue(statements.get(0).startsWith("CREATE FUNCTION"));
        assertTrue(statements.get(0).contains("$$"));
        assertEquals("SELECT 1", statements.get(1));
    }

    @Test
    void splitStatementsKeepsLanguageClauseAfterDollarQuote() {
        String sql = """
                CREATE FUNCTION demo_fn() RETURNS int AS $$
                BEGIN
                    RETURN 1;
                END;
                $$ LANGUAGE plpgsql;
                SELECT 1;
                """;
        List<String> statements = validationService.splitStatements(sql);
        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("LANGUAGE plpgsql"));
        assertEquals("SELECT 1", statements.get(1));
    }

    @Test
    void splitStatementsIgnoresLeadingCommentsBeforeProcedure() {
        String sql = """
                -- 发布存储过程
                /* header */
                CREATE OR REPLACE PROCEDURE p_demo
                AS
                begin
                    null;
                END;
                /
                SELECT 1;
                """;
        List<String> statements = validationService.splitStatements(sql);
        assertEquals(2, statements.size());
        assertTrue(statements.get(0).startsWith("CREATE OR REPLACE PROCEDURE"));
        assertEquals("SELECT 1", statements.get(1));
    }

    @Test
    void splitStatementsKeepsCreateFunctionAsOneStatement() {
        String sql = """
                CREATE OR REPLACE FUNCTION demo_fn() RETURN number
                AS
                    v number;
                begin
                    v := 1;
                    return v;
                end;
                """;
        List<String> statements = validationService.splitStatements(sql);
        assertEquals(1, statements.size());
        assertTrue(statements.get(0).contains("RETURN number"));
    }
}
