package dao;

import config.DatabaseConnection;
import models.Paciente;
import models.Responsavel;
import models.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para a hierarquia de Usuários (Single Table Pattern).
 */
public class UsuarioDAO {

    public void inserir(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuario (nome, email, senha, tipo, condicao_medica, vinculo_familiar) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getSenha());
            stmt.setString(4, usuario.obterTipo());

            if (usuario instanceof Paciente p) {
                stmt.setString(5, p.getCondicaoMedica());
                stmt.setString(6, null);
            } else if (usuario instanceof Responsavel r) {
                stmt.setString(5, null);
                stmt.setString(6, r.getVinculoFamiliar());
            } else {
                stmt.setString(5, null);
                stmt.setString(6, null);
            }

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usuario.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public Usuario buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, nome, email, senha, tipo, condicao_medica, vinculo_familiar FROM usuario WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return instanciarUsuario(rs);
                }
            }
        }
        return null;
    }

    public List<Paciente> listarPacientes() throws SQLException {
        String sql = "SELECT id, nome, email, senha, tipo, condicao_medica, vinculo_familiar " +
                "FROM usuario WHERE tipo = 'PACIENTE' ORDER BY nome";
        List<Paciente> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Usuario u = instanciarUsuario(rs);
                if (u instanceof Paciente p) {
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    public List<Usuario> listarTodos() throws SQLException {
        String sql = "SELECT id, nome, email, senha, tipo, condicao_medica, vinculo_familiar FROM usuario ORDER BY id";
        List<Usuario> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(instanciarUsuario(rs));
            }
        }
        return lista;
    }

    private Usuario instanciarUsuario(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nome = rs.getString("nome");
        String email = rs.getString("email");
        String senha = rs.getString("senha");
        String tipo = rs.getString("tipo");

        if ("PACIENTE".equalsIgnoreCase(tipo)) {
            String condicao = rs.getString("condicao_medica");
            return new Paciente(id, nome, email, senha, condicao);
        } else {
            String vinculo = rs.getString("vinculo_familiar");
            return new Responsavel(id, nome, email, senha, vinculo);
        }
    }
}
