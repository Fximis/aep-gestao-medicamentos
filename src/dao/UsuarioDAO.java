package dao;

import auth.Senhas;
import config.DatabaseConnection;
import models.Paciente;
import models.Responsavel;
import models.Usuario;

import java.sql.*;
import java.util.Arrays;
import java.util.Locale;

/** Persiste os perfis e mantém o hash restrito às consultas de autenticação. */
public class UsuarioDAO {
    static final String PERFIL = "id, nome, email, tipo, condicao_medica, vinculo_familiar";
    private static final String DESATIVADA = "!conta-desativada";

    public void inserir(Usuario usuario, String hash) throws SQLException {
        if (!Senhas.ehHash(hash)) throw new IllegalArgumentException("Credencial inválida.");
        String sql = """
                INSERT INTO usuario (nome, email, senha, tipo, condicao_medica, vinculo_familiar)
                SELECT ?, ?, ?, ?, ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE LOWER(TRIM(email)) = ?)
                """;
        String email = normalizarEmail(usuario.getEmail());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, email);
            stmt.setString(3, hash);
            stmt.setString(4, usuario.obterTipo());
            stmt.setString(5, usuario instanceof Paciente p ? p.getCondicaoMedica() : null);
            stmt.setString(6, usuario instanceof Responsavel r ? r.getVinculoFamiliar() : null);
            if (stmt.executeUpdate() != 1) throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        } catch (SQLException e) {
            // MySQL: 1062. SQLite: SQLITE_CONSTRAINT_UNIQUE (2067).
            if (e.getErrorCode() == 1062 || e.getErrorCode() == 2067
                    || (e.getErrorCode() == 19 && e.getMessage().contains("usuario.email"))) {
                throw new IllegalArgumentException("Este e-mail já está cadastrado.", e);
            }
            throw e;
        }
    }

    public Usuario autenticar(String email, char[] senha) throws SQLException {
        Usuario usuario = null;
        String hash = null;
        String sql = "SELECT " + PERFIL + ", senha FROM usuario WHERE LOWER(TRIM(email)) = ? LIMIT 2";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizarEmail(email));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    usuario = lerPerfil(rs);
                    hash = rs.getString("senha");
                    if (rs.next()) hash = null; // Não escolhe uma conta se o e-mail legado for ambíguo.
                }
            }
        }
        return Senhas.verificar(senha, hash) ? usuario : null;
    }

    public boolean alterarSenha(int usuarioId, char[] atual, char[] nova) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement consulta = conn.prepareStatement("SELECT senha FROM usuario WHERE id = ?")) {
            consulta.setInt(1, usuarioId);
            String hash;
            try (ResultSet rs = consulta.executeQuery()) {
                hash = rs.next() ? rs.getString(1) : null;
            }
            if (!Senhas.verificar(atual, hash)) return false;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE usuario SET senha = ? WHERE id = ? AND senha = ?")) {
                stmt.setString(1, Senhas.gerar(nova));
                stmt.setInt(2, usuarioId);
                stmt.setString(3, hash);
                return stmt.executeUpdate() == 1;
            }
        }
    }

    public static void migrarCredenciais(Connection conn) throws SQLException {
        try (Statement consulta = conn.createStatement();
             ResultSet rs = consulta.executeQuery("SELECT id, email, senha FROM usuario ORDER BY id");
             PreparedStatement atualizacao = conn.prepareStatement(
                     "UPDATE usuario SET senha = ? WHERE id = ? AND senha = ?")) {
            while (rs.next()) {
                String anterior = rs.getString("senha");
                if (DESATIVADA.equals(anterior) || Senhas.ehHash(anterior)) continue;
                String email = normalizarEmail(rs.getString("email"));
                boolean demonstracao = ("joao@email.com".equals(email) || "maria@email.com".equals(email))
                        && "123456".equals(anterior);
                String nova = DESATIVADA;
                if (!demonstracao && !anterior.isBlank()) {
                    char[] caracteres = anterior.toCharArray();
                    try {
                        nova = Senhas.gerar(caracteres);
                    } finally {
                        Arrays.fill(caracteres, '\0');
                    }
                }
                atualizacao.setString(1, nova);
                atualizacao.setInt(2, rs.getInt("id"));
                atualizacao.setString(3, anterior);
                atualizacao.executeUpdate();
            }
        }
    }

    static Usuario lerPerfil(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nome = rs.getString("nome");
        String email = rs.getString("email");
        return switch (rs.getString("tipo")) {
            case "PACIENTE" -> new Paciente(id, nome, email, rs.getString("condicao_medica"));
            case "RESPONSAVEL" -> new Responsavel(id, nome, email, rs.getString("vinculo_familiar"));
            default -> throw new SQLException("Perfil de usuário inválido.");
        };
    }

    public static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
