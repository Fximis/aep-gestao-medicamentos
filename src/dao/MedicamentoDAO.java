package dao;

import config.DatabaseConnection;
import models.Medicamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para a entidade Medicamento.
 * Implementa integralmente as operações de CRUD exigidas pelo Edital da AEP (Peso 0,3).
 */
public class MedicamentoDAO implements GenericDAO<Medicamento> {

    @Override
    public void inserir(Medicamento medicamento) throws SQLException {
        String sql = "INSERT INTO medicamento (usuario_id, nome, quantidade_estoque, frequencia_horas, quantidade_por_dose) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, medicamento.getUsuarioId());
            stmt.setString(2, medicamento.getNome());
            stmt.setInt(3, medicamento.getQuantidadeEstoque());
            stmt.setInt(4, medicamento.getFrequenciaHoras());
            stmt.setInt(5, medicamento.getQuantidadePorDose());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    medicamento.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public Medicamento buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, usuario_id, nome, quantidade_estoque, frequencia_horas, quantidade_por_dose " +
                "FROM medicamento WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return instanciarMedicamento(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Medicamento> listarTodos() throws SQLException {
        String sql = "SELECT id, usuario_id, nome, quantidade_estoque, frequencia_horas, quantidade_por_dose " +
                "FROM medicamento ORDER BY id";
        List<Medicamento> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(instanciarMedicamento(rs));
            }
        }
        return lista;
    }

    /**
     * Lista todos os medicamentos de um paciente específico.
     */
    public List<Medicamento> listarPorUsuario(int usuarioId) throws SQLException {
        String sql = "SELECT id, usuario_id, nome, quantidade_estoque, frequencia_horas, quantidade_por_dose " +
                "FROM medicamento WHERE usuario_id = ? ORDER BY nome";
        List<Medicamento> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(instanciarMedicamento(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public void atualizar(Medicamento medicamento) throws SQLException {
        String sql = "UPDATE medicamento SET nome = ?, quantidade_estoque = ?, frequencia_horas = ?, quantidade_por_dose = ? " +
                "WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, medicamento.getNome());
            stmt.setInt(2, medicamento.getQuantidadeEstoque());
            stmt.setInt(3, medicamento.getFrequenciaHoras());
            stmt.setInt(4, medicamento.getQuantidadePorDose());
            stmt.setInt(5, medicamento.getId());

            stmt.executeUpdate();
        }
    }

    /**
     * Ajuste manual de quantidade em estoque (RF07 e RN07).
     */
    public void atualizarEstoque(int medicamentoId, int novoEstoque) throws SQLException {
        if (novoEstoque < 0) {
            throw new IllegalArgumentException("Estoque não pode assumir valores negativos (RN04).");
        }

        String sql = "UPDATE medicamento SET quantidade_estoque = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, novoEstoque);
            stmt.setInt(2, medicamentoId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void excluir(int id) throws SQLException {
        String sqlDoses = "DELETE FROM dose WHERE medicamento_id = ?";
        String sqlMed = "DELETE FROM medicamento WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Remove as doses associadas para garantir integridade mesmo sem cascade ativo
            try (PreparedStatement stmtDoses = conn.prepareStatement(sqlDoses)) {
                stmtDoses.setInt(1, id);
                stmtDoses.executeUpdate();
            }
            try (PreparedStatement stmtMed = conn.prepareStatement(sqlMed)) {
                stmtMed.setInt(1, id);
                stmtMed.executeUpdate();
            }
        }
    }

    private Medicamento instanciarMedicamento(ResultSet rs) throws SQLException {
        return new Medicamento(
                rs.getInt("id"),
                rs.getInt("usuario_id"),
                rs.getString("nome"),
                rs.getInt("quantidade_estoque"),
                rs.getInt("frequencia_horas"),
                rs.getInt("quantidade_por_dose")
        );
    }
}
