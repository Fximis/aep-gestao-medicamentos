package config;

import dao.UsuarioDAO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.sql.*;
import java.util.Locale;

/** Escolhe o banco da execução e inicializa o esquema e as credenciais. */
public final class DatabaseConnection {
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/gestao_medicamentos"
            + "?serverTimezone=UTC&connectTimeout=2000";
    private static final String SQLITE_URL = "jdbc:sqlite:database/gestao_medicamentos.db";

    private static Boolean preferSQLite;
    private static boolean schemaInicializado;

    private DatabaseConnection() { }

    public static synchronized Connection getConnection() throws SQLException {
        Connection conn = abrirBanco();
        try {
            if (Boolean.TRUE.equals(preferSQLite)) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                    stmt.execute("PRAGMA secure_delete = ON");
                }
            }
            if (!schemaInicializado) {
                inicializarSchema(conn);
                schemaInicializado = true;
            }
            return conn;
        } catch (SQLException | RuntimeException e) {
            try { conn.close(); } catch (SQLException fechamento) { e.addSuppressed(fechamento); }
            throw e;
        }
    }

    private static Connection abrirBanco() throws SQLException {
        if (preferSQLite != null) return preferSQLite ? conectarSQLite() : conectarMySQL();

        String modo = System.getProperty("aep.banco", ambiente("AEP_BANCO", "auto")).toLowerCase(Locale.ROOT);
        if ("sqlite".equals(modo)) {
            Connection conn = conectarSQLite();
            preferSQLite = true;
            return conn;
        }
        if (!"mysql".equals(modo) && !"auto".equals(modo)) {
            throw new SQLException("AEP_BANCO deve ser auto, mysql ou sqlite.");
        }
        try {
            Connection conn = conectarMySQL();
            preferSQLite = false;
            return conn;
        } catch (SQLException e) {
            if ("mysql".equals(modo)) throw e;
            Connection conn = conectarSQLite();
            preferSQLite = true;
            return conn;
        }
    }

    private static Connection conectarMySQL() throws SQLException {
        return DriverManager.getConnection(MYSQL_URL, ambiente("AEP_MYSQL_USER", "root"),
                ambiente("AEP_MYSQL_PASSWORD", ""));
    }

    private static Connection conectarSQLite() throws SQLException {
        try {
            Files.createDirectories(Path.of("database"));
        } catch (IOException e) {
            throw new SQLException("Não foi possível preparar o diretório do SQLite.", e);
        }
        return DriverManager.getConnection(SQLITE_URL);
    }

    private static String ambiente(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null ? padrao : valor;
    }

    public static boolean isUsingSQLite() {
        return Boolean.TRUE.equals(preferSQLite);
    }

    private static void inicializarSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            if (isUsingSQLite()) {
                stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS usuario (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            nome TEXT NOT NULL, email TEXT NOT NULL UNIQUE, senha TEXT NOT NULL,
                            tipo TEXT NOT NULL, condicao_medica TEXT, vinculo_familiar TEXT
                        )
                        """);
                stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS medicamento (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            usuario_id INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                            nome TEXT NOT NULL, quantidade_estoque INTEGER NOT NULL DEFAULT 0,
                            frequencia_horas INTEGER NOT NULL, quantidade_por_dose INTEGER NOT NULL DEFAULT 1
                        )
                        """);
                stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS dose (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            medicamento_id INTEGER NOT NULL REFERENCES medicamento(id) ON DELETE CASCADE,
                            horario_previsto TEXT NOT NULL, horario_consumo TEXT,
                            status_tomado INTEGER NOT NULL DEFAULT 0
                        )
                        """);
            }
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS acesso_familiar (
                        paciente_id INTEGER NOT NULL,
                        responsavel_id INTEGER NOT NULL,
                        PRIMARY KEY (paciente_id, responsavel_id),
                        FOREIGN KEY (paciente_id) REFERENCES usuario(id) ON DELETE CASCADE,
                        FOREIGN KEY (responsavel_id) REFERENCES usuario(id) ON DELETE CASCADE
                    )
                    """);
        }
        conn.setAutoCommit(false);
        try {
            UsuarioDAO.migrarCredenciais(conn);
            conn.commit();
        } catch (SQLException | RuntimeException e) {
            try { conn.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
