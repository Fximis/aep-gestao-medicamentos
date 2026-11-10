package dao;

import config.DatabaseConnection;
import models.Dose;
import models.Medicamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para agendamento, notificação e confirmação de doses.
 * Contém a regra de ouro: baixa do estoque e recálculo com transação ACID.
 */
public class DoseDAO {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void inserir(Dose dose) throws SQLException {
        String sql = "INSERT INTO dose (medicamento_id, horario_previsto, horario_consumo, status_tomado) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, dose.getMedicamentoId());
            setTimestampParam(stmt, 2, dose.getHorarioPrevisto());
            setTimestampParam(stmt, 3, dose.getHorarioConsumo());
            stmt.setBoolean(4, dose.isStatusTomado());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    dose.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    /**
     * Gera e persiste a agenda de doses futuras para um medicamento com base no intervalo de horas.
     */
    public void gerarAgendaFutura(Medicamento medicamento, int quantidadeDoses) throws SQLException {
        LocalDateTime proximaDose = LocalDateTime.now();
        for (int i = 0; i < quantidadeDoses; i++) {
            Dose dose = new Dose(0, medicamento.getId(), proximaDose);
            inserir(dose);
            proximaDose = proximaDose.plusHours(medicamento.getFrequenciaHoras());
        }
    }

    /**
     * Lista todas as doses pendentes de um paciente (RF02 - Lembretes).
     */
    public List<Dose> listarPendentesPorPaciente(int usuarioId) throws SQLException {
        String sql = "SELECT d.id, d.medicamento_id, m.nome AS nome_medicamento, " +
                "d.horario_previsto, d.horario_consumo, d.status_tomado " +
                "FROM dose d " +
                "INNER JOIN medicamento m ON d.medicamento_id = m.id " +
                "WHERE m.usuario_id = ? AND d.status_tomado = 0 " +
                "ORDER BY d.horario_previsto ASC";

        List<Dose> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(instanciarDose(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Histórico completo de doses de um medicamento.
     */
    public List<Dose> listarPorMedicamento(int medicamentoId) throws SQLException {
        String sql = "SELECT d.id, d.medicamento_id, m.nome AS nome_medicamento, " +
                "d.horario_previsto, d.horario_consumo, d.status_tomado " +
                "FROM dose d " +
                "INNER JOIN medicamento m ON d.medicamento_id = m.id " +
                "WHERE d.medicamento_id = ? " +
                "ORDER BY d.horario_previsto ASC";

        List<Dose> lista = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, medicamentoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(instanciarDose(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Executa a confirmação formal da dose tomada com transação atômica (ACID).
     * Deduz a quantidade do estoque físico e recalcula dias restantes (RN01, RN02, RN05, RN06).
     *
     * @return Medicamento atualizado com novo estoque e autonomia
     */
    public Medicamento confirmarTomadaDose(int doseId) throws SQLException {
        String sqlSelectDose = "SELECT d.id, d.status_tomado, m.id AS med_id, m.usuario_id, m.nome, " +
                "m.quantidade_estoque, m.frequencia_horas, m.quantidade_por_dose " +
                "FROM dose d " +
                "INNER JOIN medicamento m ON d.medicamento_id = m.id " +
                "WHERE d.id = ?";

        String sqlUpdateDose = "UPDATE dose SET status_tomado = 1, horario_consumo = ? WHERE id = ?";
        String sqlUpdateEstoque = "UPDATE medicamento SET quantidade_estoque = ? WHERE id = ?";

        Connection conn = DatabaseConnection.getConnection();
        boolean autoCommitOriginal = conn.getAutoCommit();

        try {
            conn.setAutoCommit(false); // Início da Transação

            Medicamento medicamento;
            boolean jaTomada;

            // 1. Consulta dose e medicamento
            try (PreparedStatement stmtSelect = conn.prepareStatement(sqlSelectDose)) {
                stmtSelect.setInt(1, doseId);
                try (ResultSet rs = stmtSelect.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException("Dose com ID #" + doseId + " não encontrada.");
                    }
                    jaTomada = rs.getBoolean("status_tomado");
                    if (jaTomada) {
                        throw new IllegalStateException("Esta dose já foi registrada como tomada anteriormente.");
                    }

                    medicamento = new Medicamento(
                            rs.getInt("med_id"),
                            rs.getInt("usuario_id"),
                            rs.getString("nome"),
                            rs.getInt("quantidade_estoque"),
                            rs.getInt("frequencia_horas"),
                            rs.getInt("quantidade_por_dose")
                    );
                }
            }

            // 2. Valida estoque suficiente (RN04)
            if (!medicamento.reduzirEstoque(medicamento.getQuantidadePorDose())) {
                throw new IllegalStateException("Estoque insuficiente para registrar a dose de " + medicamento.getNome());
            }

            // 3. Atualiza registro da dose
            try (PreparedStatement stmtDose = conn.prepareStatement(sqlUpdateDose)) {
                setTimestampParam(stmtDose, 1, LocalDateTime.now());
                stmtDose.setInt(2, doseId);
                stmtDose.executeUpdate();
            }

            // 4. Atualiza registro do estoque no banco
            try (PreparedStatement stmtEstoque = conn.prepareStatement(sqlUpdateEstoque)) {
                stmtEstoque.setInt(1, medicamento.getQuantidadeEstoque());
                stmtEstoque.setInt(2, medicamento.getId());
                stmtEstoque.executeUpdate();
            }

            conn.commit(); // Confirmação atômica
            return medicamento;

        } catch (SQLException | RuntimeException e) {
            conn.rollback(); // Rollback em caso de erro
            throw e;
        } finally {
            conn.setAutoCommit(autoCommitOriginal);
        }
    }

    private void setTimestampParam(PreparedStatement stmt, int index, LocalDateTime ldt) throws SQLException {
        if (ldt == null) {
            stmt.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            stmt.setString(index, ldt.format(ISO_FORMATTER));
        }
    }

    private Dose instanciarDose(ResultSet rs) throws SQLException {
        LocalDateTime previsto = parseLocalDateTime(rs, "horario_previsto");
        LocalDateTime consumo = parseLocalDateTime(rs, "horario_consumo");

        return new Dose(
                rs.getInt("id"),
                rs.getInt("medicamento_id"),
                rs.getString("nome_medicamento"),
                previsto,
                consumo,
                rs.getBoolean("status_tomado")
        );
    }

    private LocalDateTime parseLocalDateTime(ResultSet rs, String colName) throws SQLException {
        String val = rs.getString(colName);
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        val = val.trim();
        try {
            if (val.contains(" ")) {
                val = val.replace(" ", "T");
            }
            if (val.length() == 16) {
                val += ":00";
            }
            return LocalDateTime.parse(val);
        } catch (Exception e) {
            try {
                Timestamp ts = rs.getTimestamp(colName);
                if (ts != null) {
                    return ts.toLocalDateTime();
                }
            } catch (Exception ignored) {
            }
            return null;
        }
    }
}
