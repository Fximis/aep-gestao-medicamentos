package dao;

import auth.Autenticacao;
import config.DatabaseConnection;
import models.Dose;
import models.Medicamento;
import models.Paciente;
import models.Responsavel;
import models.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Autorizações persistentes concedidas pelo próprio paciente e consultas de leitura do responsável. */
public final class FamiliarDAO {
    public record Acompanhamento(Paciente paciente, List<Medicamento> medicamentos, List<Dose> doses) { }

    private final MedicamentoDAO medicamentos = new MedicamentoDAO();
    private final DoseDAO doses = new DoseDAO();

    public List<Usuario> listarResponsaveis(Autenticacao.Sessao sessao) throws SQLException {
        int paciente = idDoPaciente(sessao);
        return consultarPerfis("JOIN acesso_familiar a ON a.responsavel_id = u.id "
                + "WHERE a.paciente_id = ? AND u.tipo = 'RESPONSAVEL'", paciente);
    }

    public List<Usuario> listarPacientes(Autenticacao.Sessao sessao) throws SQLException {
        int responsavel = idDoResponsavel(sessao);
        return consultarPerfis("JOIN acesso_familiar a ON a.paciente_id = u.id "
                + "WHERE a.responsavel_id = ? AND u.tipo = 'PACIENTE'", responsavel);
    }

    public void autorizar(Autenticacao.Sessao sessao, String email) throws SQLException {
        int paciente = idDoPaciente(sessao);
        String destinatario = Autenticacao.validarEmail(email);
        String sql = """
                INSERT INTO acesso_familiar (paciente_id, responsavel_id)
                SELECT ?, u.id FROM usuario u
                WHERE LOWER(TRIM(u.email)) = ? AND u.tipo = 'RESPONSAVEL'
                  AND (SELECT COUNT(*) FROM usuario WHERE LOWER(TRIM(email)) = ?) = 1
                  AND u.senha LIKE 'pbkdf2-sha256$%'
                  AND NOT EXISTS (SELECT 1 FROM acesso_familiar a
                                  WHERE a.paciente_id = ? AND a.responsavel_id = u.id)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, paciente);
            stmt.setString(2, destinatario);
            stmt.setString(3, destinatario);
            stmt.setInt(4, paciente);
            if (stmt.executeUpdate() != 1) {
                throw new IllegalArgumentException("Informe o e-mail de um responsável cadastrado que ainda não tenha acesso.");
            }
        }
    }

    public void revogar(Autenticacao.Sessao sessao, int responsavel) throws SQLException {
        int paciente = idDoPaciente(sessao);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM acesso_familiar WHERE paciente_id = ? AND responsavel_id = ?")) {
            stmt.setInt(1, paciente);
            stmt.setInt(2, responsavel);
            stmt.executeUpdate();
        }
    }

    public Acompanhamento consultar(Autenticacao.Sessao sessao, int pacienteId) throws SQLException {
        int responsavel = idDoResponsavel(sessao);
        Paciente paciente = listarPacientes(sessao).stream()
                .filter(usuario -> usuario.getId() == pacienteId)
                .map(Paciente.class::cast).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Este paciente não autorizou a consulta."));
        // Cada consulta também verifica o vínculo, inclusive se for revogado após a seleção.
        List<Medicamento> listaMedicamentos = medicamentos.listarParaResponsavel(pacienteId, responsavel);
        List<Dose> listaDoses = doses.listarParaResponsavel(pacienteId, responsavel);
        return new Acompanhamento(paciente, List.copyOf(listaMedicamentos), List.copyOf(listaDoses));
    }

    private List<Usuario> consultarPerfis(String filtro, int id) throws SQLException {
        String sql = "SELECT u.id, u.nome, u.email, u.tipo, u.condicao_medica, u.vinculo_familiar "
                + "FROM usuario u " + filtro + " ORDER BY u.nome, u.id";
        List<Usuario> perfis = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) perfis.add(UsuarioDAO.lerPerfil(rs));
            }
        }
        return perfis;
    }

    private static int idDoPaciente(Autenticacao.Sessao sessao) {
        if (!(sessao.usuario() instanceof Paciente paciente)) {
            throw new IllegalArgumentException("Somente o paciente pode gerenciar seu acesso familiar.");
        }
        return paciente.getId();
    }

    private static int idDoResponsavel(Autenticacao.Sessao sessao) {
        if (!(sessao.usuario() instanceof Responsavel responsavel)) {
            throw new IllegalArgumentException("A consulta de acompanhamento é exclusiva do responsável.");
        }
        return responsavel.getId();
    }
}
