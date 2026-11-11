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
        return buscarPorId(id, null);
    }

    public Medicamento buscarPorId(int id, Integer usuarioId) throws SQLException {
        String sql = "SELECT * FROM medicamento WHERE id = ?" + (usuarioId == null ? "" : " AND usuario_id = ?");
        List<Medicamento> lista = usuarioId == null ? consultar(sql, id) : consultar(sql, id, usuarioId);
        return lista.isEmpty() ? null : lista.getFirst();
    }

    @Override
    public List<Medicamento> listarTodos() throws SQLException {
        return consultar("SELECT * FROM medicamento ORDER BY id");
    }

    public List<Medicamento> listarPorUsuario(int usuarioId) throws SQLException {
        return consultar("SELECT * FROM medicamento WHERE usuario_id = ? ORDER BY nome", usuarioId);
    }

    List<Medicamento> listarParaResponsavel(int pacienteId, int responsavelId) throws SQLException {
        return consultar("""
                SELECT m.* FROM medicamento m
                JOIN acesso_familiar a ON a.paciente_id = m.usuario_id
                WHERE m.usuario_id = ? AND a.responsavel_id = ?
                ORDER BY m.nome, m.id
                """, pacienteId, responsavelId);
    }

    private List<Medicamento> consultar(String sql, int... parametros) throws SQLException {
        List<Medicamento> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) stmt.setInt(i + 1, parametros[i]);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(instanciarMedicamento(rs));
            }
        }
        return lista;
    }

    @Override
    public void atualizar(Medicamento medicamento) throws SQLException {
        String sql = "UPDATE medicamento SET nome = ?, quantidade_estoque = ?, frequencia_horas = ?, quantidade_por_dose = ? " +
                "WHERE id = ? AND usuario_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, medicamento.getNome());
            stmt.setInt(2, medicamento.getQuantidadeEstoque());
            stmt.setInt(3, medicamento.getFrequenciaHoras());
            stmt.setInt(4, medicamento.getQuantidadePorDose());
            stmt.setInt(5, medicamento.getId());
            stmt.setInt(6, medicamento.getUsuarioId());
            exigirRegistro(stmt.executeUpdate());
        }
    }

    /**
     * Ajuste manual de quantidade em estoque (RF07 e RN07).
     */
    public void atualizarEstoque(int medicamentoId, int novoEstoque) throws SQLException {
        atualizarEstoque(medicamentoId, novoEstoque, null);
    }

    public void atualizarEstoque(int medicamentoId, int novoEstoque, Integer usuarioId) throws SQLException {
        if (novoEstoque < 0) {
            throw new IllegalArgumentException("Estoque não pode assumir valores negativos (RN04).");
        }

        String sql = "UPDATE medicamento SET quantidade_estoque = ? WHERE id = ?"
                + (usuarioId == null ? "" : " AND usuario_id = ?");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, novoEstoque);
            stmt.setInt(2, medicamentoId);
            if (usuarioId != null) stmt.setInt(3, usuarioId);
            exigirRegistro(stmt.executeUpdate());
        }
    }

    @Override
    public void excluir(int id) throws SQLException {
        excluir(id, null);
    }

    public void excluir(int id, Integer usuarioId) throws SQLException {
        String filtro = "id = ?" + (usuarioId == null ? "" : " AND usuario_id = ?");
        String sqlDoses = "DELETE FROM dose WHERE medicamento_id IN (SELECT id FROM medicamento WHERE " + filtro + ")";
        String sqlMed = "DELETE FROM medicamento WHERE " + filtro;

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // Remove as doses associadas para garantir integridade referencial com ACID
                try (PreparedStatement stmtDoses = conn.prepareStatement(sqlDoses)) {
                    stmtDoses.setInt(1, id);
                    if (usuarioId != null) stmtDoses.setInt(2, usuarioId);
                    stmtDoses.executeUpdate();
                }
                try (PreparedStatement stmtMed = conn.prepareStatement(sqlMed)) {
                    stmtMed.setInt(1, id);
                    if (usuarioId != null) stmtMed.setInt(2, usuarioId);
                    int excluidos = stmtMed.executeUpdate();
                    if (usuarioId != null) exigirRegistro(excluidos);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommitOriginal);
            }
        }
    }

    private static void exigirRegistro(int quantidade) throws SQLException {
        if (quantidade != 1) throw new SQLException("Medicamento indisponível para esta operação.");
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
