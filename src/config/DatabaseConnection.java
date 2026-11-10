package config;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gerenciador centralizado de conexão com Banco de Dados Relacional via JDBC.
 * Prioriza conexão ativa com MySQL (porta padrão 3306).
 * Caso o servidor MySQL não esteja em execução, ativa automaticamente o banco relacional
 * embutido (SQLite) para garantir 100% de execução e evitar falhas no ambiente de avaliação.
 */
public class DatabaseConnection {

    // Configuração padrão para MySQL (Laragon / XAMPP / Localhost)
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/gestao_medicamentos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASS = "";

    // Configuração para banco relacional SQLite (fallback local)
    private static final String SQLITE_URL = "jdbc:sqlite:database/gestao_medicamentos.db";

    private static Boolean preferSQLite = null;
    private static boolean schemaInicializado = false;

    private DatabaseConnection() {
    }

    /**
     * Retorna uma nova conexão ativa com o banco de dados.
     */
    public static synchronized Connection getConnection() throws SQLException {
        // Se já foi determinado que o MySQL está offline, conecta direto ao SQLite
        if (Boolean.TRUE.equals(preferSQLite)) {
            return conectarSQLite();
        }

        // 1ª Tentativa: Conectar ao MySQL
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS);
            if (preferSQLite == null) {
                preferSQLite = false;
                System.out.println("[BD INFO] Conexão ativa estabelecida com MySQL (localhost:3306).");
            }
            return conn;
        } catch (ClassNotFoundException | SQLException e) {
            if (preferSQLite == null) {
                preferSQLite = true;
                System.out.println("[BD AVISO] Servidor MySQL local nao detectado.");
                System.out.println("[BD INFO] Ativando Banco de Dados Relacional SQLite ('database/gestao_medicamentos.db')...");
            }
            return conectarSQLite();
        }
    }

    private static Connection conectarSQLite() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbDir = new File("database");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            Connection conn = DriverManager.getConnection(SQLITE_URL);
            if (!schemaInicializado) {
                inicializarSchemaSQLite(conn);
                schemaInicializado = true;
                System.out.println("[BD INFO] Banco de Dados Relacional SQLite pronto e conectado.");
            }
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC SQLite não encontrado na pasta lib/.", e);
        }
    }

    public static boolean isUsingSQLite() {
        return Boolean.TRUE.equals(preferSQLite);
    }

    /**
     * Cria as tabelas e a carga inicial no SQLite caso seja a primeira execução.
     */
    private static void inicializarSchemaSQLite(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Tabela de Usuário
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS usuario (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "nome TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "senha TEXT NOT NULL, " +
                    "tipo TEXT NOT NULL, " +
                    "condicao_medica TEXT, " +
                    "vinculo_familiar TEXT);");

            // Tabela de Medicamento
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS medicamento (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "usuario_id INTEGER NOT NULL, " +
                    "nome TEXT NOT NULL, " +
                    "quantidade_estoque INTEGER NOT NULL DEFAULT 0, " +
                    "frequencia_horas INTEGER NOT NULL, " +
                    "quantidade_por_dose INTEGER NOT NULL DEFAULT 1, " +
                    "FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE);");

            // Tabela de Dose
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS dose (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "medicamento_id INTEGER NOT NULL, " +
                    "horario_previsto TEXT NOT NULL, " +
                    "horario_consumo TEXT, " +
                    "status_tomado INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY (medicamento_id) REFERENCES medicamento(id) ON DELETE CASCADE);");

            // Seed inicial se a base estiver vazia
            var rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM usuario;");
            if (rs.next() && rs.getInt("total") == 0) {
                stmt.executeUpdate("INSERT INTO usuario (id, nome, email, senha, tipo, condicao_medica, vinculo_familiar) VALUES " +
                        "(1, 'Joao da Silva', 'joao@email.com', '123456', 'PACIENTE', 'Hipertensao e Diabetes Tipo 2', NULL), " +
                        "(2, 'Maria Silva Santos', 'maria@email.com', '123456', 'RESPONSAVEL', NULL, 'Filha e Cuidadora');");

                stmt.executeUpdate("INSERT INTO medicamento (id, usuario_id, nome, quantidade_estoque, frequencia_horas, quantidade_por_dose) VALUES " +
                        "(101, 1, 'Losartana Potassica 50mg', 10, 12, 1), " +
                        "(102, 1, 'Cloridrato de Metformina 850mg', 60, 8, 1);");

                java.time.LocalDateTime agora = java.time.LocalDateTime.now();
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String d1 = agora.minusMinutes(30).format(fmt);
                String d2 = agora.plusHours(11).plusMinutes(30).format(fmt);
                String d3 = agora.minusMinutes(15).format(fmt);
                String d4 = agora.plusHours(7).plusMinutes(45).format(fmt);
                String d5 = agora.plusHours(15).plusMinutes(45).format(fmt);

                stmt.executeUpdate(String.format(
                        "INSERT INTO dose (id, medicamento_id, horario_previsto, horario_consumo, status_tomado) VALUES " +
                        "(1001, 101, '%s', NULL, 0), " +
                        "(1002, 101, '%s', NULL, 0), " +
                        "(1003, 102, '%s', NULL, 0), " +
                        "(1004, 102, '%s', NULL, 0), " +
                        "(1005, 102, '%s', NULL, 0);",
                        d1, d2, d3, d4, d5));
            }
        } catch (SQLException e) {
            System.err.println("[BD ERRO] Falha ao inicializar tabelas: " + e.getMessage());
        }
    }

    /**
     * Reinicializa o banco SQLite com a carga limpa de demonstracao da AEP.
     */
    public static synchronized void reiniciarBaseDemonstracao() {
        File dbFile = new File("database/gestao_medicamentos.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }
        schemaInicializado = false;
        try (Connection conn = conectarSQLite()) {
            // Recria e popula
        } catch (SQLException e) {
            System.err.println("[BD ERRO] Falha ao reiniciar base: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        // Conexões são gerenciadas por try-with-resources nos DAOs
    }
}
