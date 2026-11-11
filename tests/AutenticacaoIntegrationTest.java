import auth.Autenticacao;
import auth.Senhas;
import config.DatabaseConnection;
import dao.DoseDAO;
import dao.FamiliarDAO;
import dao.MedicamentoDAO;
import models.Medicamento;
import models.Paciente;
import models.Responsavel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;

public class AutenticacaoIntegrationTest {
    private static final String SENHA_TESTE = "Senha só para testes";
    private static int total;

    public static void main(String[] args) throws Exception {
        System.setProperty("aep.banco", "sqlite");
        prepararBancoLegado();
        Autenticacao auth = new Autenticacao();
        var legado = auth.entrar("antigo@example.com", "credencial antiga".toCharArray());
        verificar(legado.usuario().getId() == 41 && Senhas.ehHash(hash("antigo@example.com")),
                "Migração preserva o ID e converte senha em hash");
        verificar(new MedicamentoDAO().listarPorUsuario(41).size() == 1, "Migração preserva dados existentes");
        rejeitar(IllegalArgumentException.class, () -> auth.entrar("joao@email.com", "123456".toCharArray()));
        verificar(!hash("joao@email.com").equals("123456"), "Senha pública de demonstração desativada");

        cadastrar(auth, "Paciente Um", " UM@example.com ", Autenticacao.Perfil.PACIENTE);
        cadastrar(auth, "Paciente Dois", "dois@example.com", Autenticacao.Perfil.PACIENTE);
        cadastrar(auth, "Responsável", "familia@example.com", Autenticacao.Perfil.RESPONSAVEL);
        String hashUm = hash("um@example.com");
        verificar(Senhas.ehHash(hashUm) && !hashUm.contains(SENHA_TESTE)
                && !hashUm.equals(hash("dois@example.com")), "Hashes com salts distintos para a mesma senha");
        rejeitar(IllegalArgumentException.class,
                () -> cadastrar(auth, "Duplicado", "UM@EXAMPLE.COM", Autenticacao.Perfil.PACIENTE));
        rejeitar(IllegalArgumentException.class,
                () -> cadastrar(auth, "E-mail inválido", "sem-dominio", Autenticacao.Perfil.PACIENTE));
        rejeitar(IllegalArgumentException.class,
                () -> auth.cadastrar("Curta", "curta@example.com", Autenticacao.Perfil.PACIENTE, "",
                        "curta".toCharArray(), "curta".toCharArray()));
        rejeitar(IllegalArgumentException.class,
                () -> auth.cadastrar("Divergente", "dif@example.com", Autenticacao.Perfil.PACIENTE, "",
                        SENHA_TESTE.toCharArray(), "Outra senha de teste".toCharArray()));

        var um = entrar(auth, "UM@EXAMPLE.COM");
        var dois = entrar(auth, "dois@example.com");
        var responsavel = entrar(auth, "familia@example.com");
        verificar(um.usuario() instanceof Paciente && responsavel.usuario() instanceof Responsavel,
                "Login recupera o perfil e aceita e-mail sem distinção de maiúsculas");
        verificar(um.usuario().getId() != dois.usuario().getId(), "Identidades independentes");

        MedicamentoDAO medicamentos = new MedicamentoDAO();
        DoseDAO doses = new DoseDAO();
        FamiliarDAO familia = new FamiliarDAO();
        int dono = um.usuario().getId();
        int outro = dois.usuario().getId();
        int cuidador = responsavel.usuario().getId();
        Medicamento m1 = new Medicamento(dono, "Medicamento do primeiro paciente", 10, 12, 1);
        Medicamento m2 = new Medicamento(outro, "Medicamento do segundo paciente", 10, 12, 1);
        doses.cadastrarTratamento(m1, 2);
        doses.cadastrarTratamento(m2, 2);
        verificar(medicamentos.buscarPorId(m1.getId(), outro) == null, "Consulta por ID filtra proprietário");
        rejeitar(SQLException.class, () -> medicamentos.atualizarEstoque(m1.getId(), 99, outro));
        rejeitar(SQLException.class, () -> medicamentos.excluir(m1.getId(), outro));
        int dose1 = doses.listarPorMedicamento(m1.getId()).getFirst().getId();
        int dose2 = doses.listarPorMedicamento(m2.getId()).getFirst().getId();
        rejeitar(IllegalStateException.class, () -> doses.confirmarTomadaDose(dose1, outro));
        rejeitar(IllegalStateException.class, () -> doses.confirmarTodasDosesAgora(List.of(dose1, dose2), dono));
        verificar(medicamentos.buscarPorId(m1.getId(), dono).getQuantidadeEstoque() == 10
                && doses.listarPorMedicamento(m1.getId()).stream().noneMatch(d -> d.isStatusTomado()),
                "Acesso negado preserva estoque e desfaz alterações parciais do lote");

        verificar(familia.listarPacientes(responsavel).isEmpty(), "Responsável começa sem acesso");
        rejeitar(IllegalArgumentException.class, () -> familia.consultar(responsavel, dono));
        rejeitar(IllegalArgumentException.class, () -> familia.autorizar(responsavel, "familia@example.com"));
        familia.autorizar(um, "FAMILIA@example.com");
        verificar(familia.listarResponsaveis(um).size() == 1
                && familia.listarPacientes(responsavel).size() == 1, "Consentimento persistido entre sessões");
        var resumo = familia.consultar(responsavel, dono);
        verificar(resumo.medicamentos().size() == 1 && resumo.doses().size() == 2,
                "Responsável consulta apenas o tratamento autorizado");
        rejeitar(IllegalArgumentException.class, () -> familia.consultar(responsavel, outro));
        familia.revogar(um, cuidador);
        rejeitar(IllegalArgumentException.class, () -> familia.consultar(responsavel, dono));

        char[] nova = "Uma senha nova de teste".toCharArray();
        rejeitar(IllegalArgumentException.class,
                () -> auth.alterarSenha(um, "incorreta".toCharArray(), nova, nova));
        auth.alterarSenha(um, SENHA_TESTE.toCharArray(), nova, nova);
        rejeitar(IllegalArgumentException.class, () -> entrar(auth, "um@example.com"));
        verificar(auth.entrar("um@example.com", nova).usuario().getId() == dono, "Nova senha autentica");
        um.encerrar();
        rejeitar(IllegalStateException.class, () -> familia.listarResponsaveis(um));

        Autenticacao limite = new Autenticacao();
        String erroAusente = rejeitar(IllegalArgumentException.class, () -> entrar(limite, "ausente@example.com"));
        String erroIncorreto = rejeitar(IllegalArgumentException.class, () -> entrar(limite, "um@example.com"));
        verificar(erroAusente.equals(erroIncorreto), "Mesma mensagem para conta ausente e senha incorreta");
        for (int i = 0; i < 3; i++) rejeitar(IllegalArgumentException.class, () -> entrar(limite, "um@example.com"));
        rejeitar(IllegalStateException.class, () -> entrar(limite, "dois@example.com"));
        System.out.println("OK: " + total + " verificações de autenticação e privacidade com SQLite.");
    }

    private static void prepararBancoLegado() throws Exception {
        Path arquivo = Path.of("database/gestao_medicamentos.db");
        if (Files.exists(arquivo)) throw new IllegalStateException("Execute em um diretório temporário vazio.");
        Files.createDirectories(arquivo.getParent());
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE usuario (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL, senha TEXT NOT NULL, tipo TEXT NOT NULL,
                    condicao_medica TEXT, vinculo_familiar TEXT)
                    """);
            stmt.executeUpdate("""
                    INSERT INTO usuario (id, nome, email, senha, tipo) VALUES
                    (41, 'Conta anterior', 'antigo@example.com', 'credencial antiga', 'PACIENTE'),
                    (42, 'Demonstração anterior', 'joao@email.com', '123456', 'PACIENTE')
                    """);
            stmt.executeUpdate("""
                    CREATE TABLE medicamento (id INTEGER PRIMARY KEY AUTOINCREMENT, usuario_id INTEGER NOT NULL,
                    nome TEXT NOT NULL, quantidade_estoque INTEGER NOT NULL, frequencia_horas INTEGER NOT NULL,
                    quantidade_por_dose INTEGER NOT NULL, FOREIGN KEY(usuario_id) REFERENCES usuario(id))
                    """);
            stmt.executeUpdate("INSERT INTO medicamento VALUES (1, 41, 'Tratamento antigo', 10, 12, 1)");
        }
    }

    private static String hash(String email) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT senha FROM usuario WHERE email = ?")) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) throw new AssertionError("Conta de teste ausente");
                return rs.getString(1);
            }
        }
    }

    private static void cadastrar(Autenticacao auth, String nome, String email, Autenticacao.Perfil perfil)
            throws SQLException {
        auth.cadastrar(nome, email, perfil, "", SENHA_TESTE.toCharArray(), SENHA_TESTE.toCharArray());
    }

    private static Autenticacao.Sessao entrar(Autenticacao auth, String email) throws SQLException {
        return auth.entrar(email, SENHA_TESTE.toCharArray());
    }

    private static String rejeitar(Class<? extends Exception> tipo, Acao acao) throws Exception {
        try {
            acao.executar();
        } catch (Exception e) {
            if (!tipo.isInstance(e)) throw e;
            total++;
            return e.getMessage();
        }
        throw new AssertionError("Esperava rejeição " + tipo.getSimpleName());
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
        total++;
    }

    @FunctionalInterface
    private interface Acao { void executar() throws Exception; }
}
