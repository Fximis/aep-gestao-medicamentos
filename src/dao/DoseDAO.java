package dao;

import config.DatabaseConnection;
import models.Dose;
import models.Medicamento;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Agenda e confirmacao de doses: contribuicao de Carlos Henrique.
 * RF02, RF03, RF04, RN01 e RN06. Mantem as APIs usadas pela GUI original.
 */
public class DoseDAO {
    private static final DateTimeFormatter SQL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SELECT_DOSES = """
            SELECT d.id, d.medicamento_id, m.nome AS nome_medicamento,
                   d.horario_previsto, d.horario_consumo, d.status_tomado
            FROM dose d JOIN medicamento m ON m.id = d.medicamento_id
            """;

    public void inserir(Dose dose) throws SQLException {
        Objects.requireNonNull(dose, "Informe a dose.");
        if (dose.getMedicamentoId() <= 0 || dose.getHorarioPrevisto() == null)
            throw new IllegalArgumentException("Medicamento e horario previsto sao obrigatorios.");
        if (dose.isStatusTomado() || dose.getHorarioConsumo() != null)
            throw new IllegalArgumentException("Use confirmarTomadaDose para registrar consumo.");
        int id = emTransacao(conn -> inserirPendente(conn, dose.getMedicamentoId(), dose.getHorarioPrevisto()));
        dose.setId(id);
    }

    /** Cadastro e primeira agenda sao salvos juntos, ou nada e salvo. */
    public void cadastrarTratamento(Medicamento m, int quantidadeDoses) throws SQLException {
        validarTratamento(m, quantidadeDoses);
        int id = emTransacao(conn -> {
            int novoId;
            try (PreparedStatement stmt = conn.prepareStatement("""
                    INSERT INTO medicamento
                    (usuario_id, nome, quantidade_estoque, frequencia_horas, quantidade_por_dose)
                    VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, m.getUsuarioId());
                stmt.setString(2, m.getNome());
                stmt.setInt(3, m.getQuantidadeEstoque());
                stmt.setInt(4, m.getFrequenciaHoras());
                stmt.setInt(5, m.getQuantidadePorDose());
                exigirUmaLinha(stmt.executeUpdate());
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Banco nao retornou o ID do medicamento.");
                    novoId = rs.getInt(1);
                }
            }
            inserirAgenda(conn, novoId, LocalDateTime.now(), m.getFrequenciaHoras(), quantidadeDoses);
            return novoId;
        });
        m.setId(id); // Somente depois do commit.
    }

    /** Altera posologia e refaz pendencias, preservando estoque e doses ja tomadas. */
    public void atualizarTratamento(Medicamento m, int quantidadeDoses) throws SQLException {
        validarTratamento(m, quantidadeDoses);
        emTransacao(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    UPDATE medicamento SET nome = ?, frequencia_horas = ?, quantidade_por_dose = ?
                    WHERE id = ? AND usuario_id = ?
                    """)) {
                stmt.setString(1, m.getNome());
                stmt.setInt(2, m.getFrequenciaHoras());
                stmt.setInt(3, m.getQuantidadePorDose());
                stmt.setInt(4, m.getId());
                stmt.setInt(5, m.getUsuarioId());
                stmt.executeUpdate();
            }
            buscarMedicamento(conn, m.getId(), m.getUsuarioId());
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM dose WHERE medicamento_id = ? AND status_tomado = FALSE")) {
                stmt.setInt(1, m.getId());
                stmt.executeUpdate();
            }
            inserirAgenda(conn, m.getId(), LocalDateTime.now().plusHours(m.getFrequenciaHoras()),
                    m.getFrequenciaHoras(), quantidadeDoses);
            return null;
        });
    }

    /** Altera somente o nome, preservando estoque e agenda. */
    public void atualizarNome(Medicamento m) throws SQLException {
        validarTratamento(m, 1);
        emTransacao(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE medicamento SET nome = ? WHERE id = ? AND usuario_id = ?")) {
                stmt.setString(1, m.getNome());
                stmt.setInt(2, m.getId());
                stmt.setInt(3, m.getUsuarioId());
                stmt.executeUpdate();
            }
            buscarMedicamento(conn, m.getId(), m.getUsuarioId());
            return null;
        });
    }

    public void gerarAgendaFutura(Medicamento m, int quantidadeDoses) throws SQLException {
        validarTratamento(m, quantidadeDoses);
        emTransacao(conn -> {
            Medicamento atual = buscarMedicamento(conn, m.getId(), m.getUsuarioId());
            inserirAgenda(conn, atual.getId(), proximoHorario(conn, atual.getId(), atual.getFrequenciaHoras()),
                    atual.getFrequenciaHoras(), quantidadeDoses);
            return null;
        });
    }

    public List<Dose> listarPendentesPorPaciente(int usuarioId) throws SQLException {
        return consultarDoses(SELECT_DOSES + """
                WHERE m.usuario_id = ? AND d.status_tomado = FALSE
                ORDER BY d.horario_previsto ASC, d.id ASC
                """, usuarioId);
    }

    public List<Dose> listarPorUsuario(int usuarioId) throws SQLException {
        return consultarDoses(SELECT_DOSES +
                " WHERE m.usuario_id = ? ORDER BY d.horario_previsto DESC, d.id DESC", usuarioId);
    }

    List<Dose> listarParaResponsavel(int pacienteId, int responsavelId) throws SQLException {
        return consultarDoses(SELECT_DOSES + """
                JOIN acesso_familiar a ON a.paciente_id = m.usuario_id
                WHERE m.usuario_id = ? AND a.responsavel_id = ?
                ORDER BY d.horario_previsto DESC, d.id DESC
                """, pacienteId, responsavelId);
    }

    public List<Dose> listarPorMedicamento(int medicamentoId) throws SQLException {
        return consultarDoses(SELECT_DOSES +
                " WHERE d.medicamento_id = ? ORDER BY d.horario_previsto, d.id", medicamentoId);
    }

    public List<Dose> listarHistoricoRecente(int usuarioId, int limite) throws SQLException {
        if (limite <= 0) throw new IllegalArgumentException("O limite deve ser positivo.");
        return consultarDoses(SELECT_DOSES + """
                WHERE m.usuario_id = ? AND d.status_tomado = TRUE
                ORDER BY d.horario_consumo DESC, d.id DESC LIMIT ?
                """, usuarioId, limite);
    }

    public List<Dose> listarDosesParaTomarAgora(int usuarioId) throws SQLException {
        return listarPendentesPorPaciente(usuarioId).stream().filter(Dose::isDevidaAgora).toList();
    }

    public List<Dose> listarProximasHoje(int usuarioId) throws SQLException {
        return listarPendentesPorPaciente(usuarioId).stream()
                .filter(d -> !d.isDevidaAgora() && d.isHoje()).toList();
    }

    public List<Dose> listarProximosDias(int usuarioId) throws SQLException {
        return listarPendentesPorPaciente(usuarioId).stream()
                .filter(d -> !d.isDevidaAgora() && !d.isHoje()).toList();
    }

    /** API anterior preservada para a GUI; o console usa a sobrecarga com paciente. */
    public Medicamento confirmarTomadaDose(int doseId) throws SQLException {
        return emTransacao(conn -> confirmarInternamente(conn, doseId, null));
    }

    public Medicamento confirmarTomadaDose(int doseId, int pacienteId) throws SQLException {
        if (pacienteId <= 0) throw new IllegalArgumentException("Paciente invalido.");
        return emTransacao(conn -> confirmarInternamente(conn, doseId, pacienteId));
    }

    public List<Medicamento> confirmarTodasDosesAgora(List<Integer> doseIds) throws SQLException {
        return confirmarLote(doseIds, null);
    }

    public List<Medicamento> confirmarTodasDosesAgora(List<Integer> doseIds, int pacienteId) throws SQLException {
        if (pacienteId <= 0) throw new IllegalArgumentException("Paciente inválido.");
        return confirmarLote(doseIds, pacienteId);
    }

    private List<Medicamento> confirmarLote(List<Integer> doseIds, Integer pacienteId) throws SQLException {
        Objects.requireNonNull(doseIds, "Informe as doses.");
        if (doseIds.isEmpty()) return List.of();
        if (doseIds.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(doseIds).size() != doseIds.size())
            throw new IllegalArgumentException("Informe IDs positivos, sem repeticao.");
        List<Integer> ordenados = new ArrayList<>(doseIds);
        Collections.sort(ordenados);
        return emTransacao(conn -> {
            List<Medicamento> resultado = new ArrayList<>();
            for (int id : ordenados) resultado.add(confirmarInternamente(conn, id, pacienteId));
            return resultado;
        });
    }

    private Medicamento confirmarInternamente(Connection conn, int doseId, Integer pacienteId)
            throws SQLException {
        if (doseId <= 0) throw new IllegalArgumentException("Dose invalida.");
        // Atualizacao condicional: duas confirmacoes nao baixam a mesma dose duas vezes.
        String sql = """
                UPDATE dose SET status_tomado = TRUE, horario_consumo = ?
                WHERE id = ? AND status_tomado = FALSE
                """ + (pacienteId == null ? "" :
                " AND medicamento_id IN (SELECT id FROM medicamento WHERE usuario_id = ?)");
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().format(SQL_DATE));
            stmt.setInt(2, doseId);
            if (pacienteId != null) stmt.setInt(3, pacienteId);
            if (stmt.executeUpdate() != 1)
                throw new IllegalStateException("Dose inexistente, ja confirmada ou nao pertencente ao paciente.");
        }
        int medicamentoId;
        try (PreparedStatement stmt = conn.prepareStatement("SELECT medicamento_id FROM dose WHERE id = ?")) {
            stmt.setInt(1, doseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new SQLException("Dose nao encontrada apos atualizacao.");
                medicamentoId = rs.getInt(1);
            }
        }
        // Subtracao no banco evita gravar um estoque obsoleto e impede valores negativos.
        try (PreparedStatement stmt = conn.prepareStatement("""
                UPDATE medicamento SET quantidade_estoque = quantidade_estoque - quantidade_por_dose
                WHERE id = ? AND quantidade_por_dose > 0 AND frequencia_horas > 0
                  AND quantidade_estoque >= quantidade_por_dose
                """)) {
            stmt.setInt(1, medicamentoId);
            if (stmt.executeUpdate() != 1)
                throw new IllegalStateException("Estoque insuficiente ou posologia invalida. Dose nao confirmada.");
        }
        Medicamento atualizado = buscarMedicamento(conn, medicamentoId, pacienteId);
        agendarProximaDoseSeNecessario(conn, atualizado);
        return atualizado;
    }

    private void agendarProximaDoseSeNecessario(Connection conn, Medicamento m) throws SQLException {
        int pendentes;
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM dose WHERE medicamento_id = ? AND status_tomado = FALSE")) {
            stmt.setInt(1, m.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new SQLException("Falha na consulta de pendencias.");
                pendentes = rs.getInt(1);
            }
        }
        if (pendentes < 3)
            inserirPendente(conn, m.getId(), proximoHorario(conn, m.getId(), m.getFrequenciaHoras()));
    }

    private LocalDateTime proximoHorario(Connection conn, int medicamentoId, int frequencia) throws SQLException {
        LocalDateTime agora = LocalDateTime.now();
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT MAX(horario_previsto) FROM dose WHERE medicamento_id = ?")) {
            stmt.setInt(1, medicamentoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime ultimo = lerData(rs.getString(1));
                    if (ultimo != null) {
                        LocalDateTime seguinte = ultimo.plusHours(frequencia);
                        return seguinte.isAfter(agora) ? seguinte : agora.plusHours(frequencia);
                    }
                }
            }
        }
        return agora;
    }

    private void inserirAgenda(Connection conn, int medicamentoId, LocalDateTime inicio,
                               int frequencia, int quantidade) throws SQLException {
        for (int i = 0; i < quantidade; i++)
            inserirPendente(conn, medicamentoId, inicio.plusHours((long) frequencia * i));
    }

    private int inserirPendente(Connection conn, int medicamentoId, LocalDateTime horario) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO dose (medicamento_id, horario_previsto, horario_consumo, status_tomado)
                VALUES (?, ?, NULL, FALSE)
                """, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, medicamentoId);
            stmt.setString(2, horario.format(SQL_DATE));
            exigirUmaLinha(stmt.executeUpdate());
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("Banco nao retornou o ID da dose.");
                return rs.getInt(1);
            }
        }
    }

    private Medicamento buscarMedicamento(Connection conn, int id, Integer pacienteId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM medicamento WHERE id = ?" + (pacienteId == null ? "" : " AND usuario_id = ?"))) {
            stmt.setInt(1, id);
            if (pacienteId != null) stmt.setInt(2, pacienteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Medicamento nao encontrado para o paciente.");
                return new Medicamento(rs.getInt("id"), rs.getInt("usuario_id"), rs.getString("nome"),
                        rs.getInt("quantidade_estoque"), rs.getInt("frequencia_horas"), rs.getInt("quantidade_por_dose"));
            }
        }
    }

    private List<Dose> consultarDoses(String sql, int... parametros) throws SQLException {
        List<Dose> resultado = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) stmt.setInt(i + 1, parametros[i]);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    resultado.add(new Dose(rs.getInt("id"), rs.getInt("medicamento_id"),
                            rs.getString("nome_medicamento"), lerData(rs.getString("horario_previsto")),
                            lerData(rs.getString("horario_consumo")), rs.getBoolean("status_tomado")));
            }
        }
        return resultado;
    }

    private LocalDateTime lerData(String valor) throws SQLException {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalDateTime.parse(valor.trim().replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            throw new SQLException("Data invalida armazenada no banco: " + valor, e);
        }
    }

    private void validarTratamento(Medicamento m, int quantidadeDoses) {
        Objects.requireNonNull(m, "Informe o medicamento.");
        if (m.getUsuarioId() <= 0 || m.getNome() == null || m.getNome().isBlank()
                || m.getNome().length() > 100 || m.getQuantidadeEstoque() < 0
                || m.getFrequenciaHoras() <= 0 || m.getQuantidadePorDose() <= 0 || quantidadeDoses <= 0)
            throw new IllegalArgumentException("Confira paciente, nome (ate 100 caracteres), estoque e posologia.");
    }

    private void exigirUmaLinha(int quantidade) throws SQLException {
        if (quantidade != 1) throw new SQLException("A operacao nao alterou exatamente um registro.");
    }

    @FunctionalInterface
    private interface Trabalho<T> {
        T executar(Connection conn) throws SQLException;
    }

    private <T> T emTransacao(Trabalho<T> trabalho) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                T resultado = trabalho.executar(conn);
                conn.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try { conn.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }
}
