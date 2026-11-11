import config.DatabaseConnection;
import dao.DoseDAO;
import dao.MedicamentoDAO;
import dao.UsuarioDAO;
import models.Dose;
import models.Medicamento;
import views.ConsoleView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/** Testes de integracao com SQLite REAL, executados em diretorio temporario pelo script. */
public class CarlosHenriqueIntegrationTest {
    private static final DoseDAO doses = new DoseDAO();
    private static final MedicamentoDAO medicamentos = new MedicamentoDAO();
    private static int total;

    public static void main(String[] args) throws Exception {
        // O projeto original escolhe MySQL automaticamente. Para isolar o teste, fixa SQLite.
        Field preferencia = DatabaseConnection.class.getDeclaredField("preferSQLite");
        preferencia.setAccessible(true);
        preferencia.set(null, Boolean.TRUE);
        try (Connection conn = DatabaseConnection.getConnection()) {
            check(conn.getMetaData().getURL().startsWith("jdbc:sqlite:"), "Conexao real SQLite");
        }

        Medicamento m = novo("Teste consumo", 36, 8, 2);
        List<Dose> agenda = doses.listarPorMedicamento(m.getId());
        check(agenda.size() == 4, "Create persiste medicamento e quatro doses");
        check(medicamentos.buscarPorId(m.getId()).getQuantidadeEstoque() == 36,
                "Agendamento nao baixa estoque sem confirmacao");
        check(agenda.get(1).getHorarioPrevisto().equals(agenda.get(0).getHorarioPrevisto().plusHours(8)),
                "Agenda respeita intervalo");
        List<Dose> pendentes = doses.listarPendentesPorPaciente(1);
        check(pendentes.stream().allMatch(d -> !d.isStatusTomado()), "Listagem retorna somente pendentes");
        boolean ordenada = true;
        for (int i = 1; i < pendentes.size(); i++)
            ordenada &= !pendentes.get(i).getHorarioPrevisto().isBefore(pendentes.get(i - 1).getHorarioPrevisto());
        check(ordenada, "Pendencias ordenadas pelo horario previsto");

        Medicamento confirmado = doses.confirmarTomadaDose(agenda.get(0).getId(), 1);
        check(confirmado.getQuantidadeEstoque() == 34 && confirmado.calcularDiasRestantes() == 5,
                "Baixa quantidade por dose e recalcula autonomia");
        check(confirmado.verificarAlertaRecompra(), "Alerta e exibivel em cinco dias");
        check(!new Medicamento(1, "Seguro", 36, 8, 2).verificarAlertaRecompra(),
                "Acima de cinco dias nao dispara alerta");
        Dose consumida = doses.listarPorMedicamento(m.getId()).stream()
                .filter(d -> d.getId() == agenda.get(0).getId()).findFirst().orElseThrow();
        check(consumida.isStatusTomado() && consumida.getHorarioConsumo() != null,
                "Confirma status e horario real de consumo");
        falha(() -> doses.confirmarTomadaDose(agenda.get(0).getId(), 1), "Bloqueia dose repetida");
        check(medicamentos.buscarPorId(m.getId()).getQuantidadeEstoque() == 34,
                "Confirmacao repetida nao baixa estoque novamente");

        Medicamento insuficiente = novo("Teste sem estoque", 1, 12, 2);
        int doseInsuficiente = doses.listarPorMedicamento(insuficiente.getId()).get(0).getId();
        falha(() -> doses.confirmarTomadaDose(doseInsuficiente, 1), "Bloqueia estoque insuficiente");
        check(medicamentos.buscarPorId(insuficiente.getId()).getQuantidadeEstoque() == 1
                && doses.listarPorMedicamento(insuficiente.getId()).stream().noneMatch(Dose::isStatusTomado),
                "Rollback preserva estoque, status e horario em falha");
        falha(() -> doses.confirmarTomadaDose(agenda.get(1).getId(), 99999),
                "Bloqueia dose de outro paciente");
        falha(() -> doses.confirmarTomadaDose(Integer.MAX_VALUE, 1), "Bloqueia dose inexistente");

        int antes = medicamentos.listarTodos().size();
        executarSQL("CREATE TRIGGER falhar_cadastro BEFORE INSERT ON dose BEGIN SELECT RAISE(ABORT, 'teste'); END");
        falha(() -> novo("Nao deve persistir", 10, 12, 1), "Falha SQL na agenda cancela cadastro");
        executarSQL("DROP TRIGGER falhar_cadastro");
        check(medicamentos.listarTodos().size() == antes, "Cadastro parcial nao fica no banco");

        Medicamento rollback = novo("Falha na renovacao", 10, 12, 1);
        List<Dose> agendaRollback = doses.listarPorMedicamento(rollback.getId());
        executarSQL("DELETE FROM dose WHERE medicamento_id = " + rollback.getId()
                + " AND id <> " + agendaRollback.get(0).getId());
        executarSQL("CREATE TRIGGER falhar_renovacao BEFORE INSERT ON dose BEGIN SELECT RAISE(ABORT, 'teste'); END");
        falha(() -> doses.confirmarTomadaDose(agendaRollback.get(0).getId(), 1),
                "Falha ao renovar agenda aborta confirmacao");
        executarSQL("DROP TRIGGER falhar_renovacao");
        check(medicamentos.buscarPorId(rollback.getId()).getQuantidadeEstoque() == 10
                && !doses.listarPorMedicamento(rollback.getId()).get(0).isStatusTomado(),
                "Rollback SQL desfaz baixa e confirmacao");

        m = medicamentos.buscarPorId(m.getId());
        List<Dose> antesNome = doses.listarPorMedicamento(m.getId());
        m.setNome("Nome atualizado");
        doses.atualizarNome(m);
        check(medicamentos.buscarPorId(m.getId()).getNome().equals("Nome atualizado")
                && doses.listarPorMedicamento(m.getId()).get(1).getHorarioPrevisto()
                    .equals(antesNome.get(1).getHorarioPrevisto()), "Update de nome preserva agenda");
        m.setFrequenciaHoras(12);
        doses.atualizarTratamento(m, 4);
        List<Dose> depoisUpdate = doses.listarPorMedicamento(m.getId());
        check(depoisUpdate.stream().filter(Dose::isStatusTomado).count() == 1
                && depoisUpdate.stream().filter(d -> !d.isStatusTomado()).count() == 4
                && medicamentos.buscarPorId(m.getId()).getQuantidadeEstoque() == 34,
                "Update de posologia refaz pendencias e preserva historico e estoque");
        List<Dose> novas = depoisUpdate.stream().filter(d -> !d.isStatusTomado()).toList();
        check(novas.get(1).getHorarioPrevisto().equals(novas.get(0).getHorarioPrevisto().plusHours(12)),
                "Nova agenda usa novo intervalo");

        medicamentos.atualizarEstoque(m.getId(), 40);
        check(new MedicamentoDAO().buscarPorId(m.getId()).getQuantidadeEstoque() == 40,
                "Ajuste persiste em nova conexao");
        int idAtual = m.getId();
        falha(() -> medicamentos.atualizarEstoque(idAtual, -1), "Bloqueia ajuste negativo");

        Medicamento lote = novo("Lote atomico", 1, 12, 1);
        List<Dose> loteDoses = doses.listarPorMedicamento(lote.getId());
        falha(() -> doses.confirmarTodasDosesAgora(List.of(loteDoses.get(0).getId(), loteDoses.get(1).getId())),
                "Lote falha quando estoque nao atende todas as doses");
        check(medicamentos.buscarPorId(lote.getId()).getQuantidadeEstoque() == 1
                && doses.listarPorMedicamento(lote.getId()).stream().noneMatch(Dose::isStatusTomado),
                "Lote e atomico");
        falha(() -> doses.confirmarTodasDosesAgora(List.of(loteDoses.get(0).getId(), loteDoses.get(0).getId())),
                "Lote rejeita IDs repetidos");

        Medicamento concorrente = novo("Concorrencia mesma dose", 10, 12, 1);
        int mesmoId = doses.listarPorMedicamento(concorrente.getId()).get(0).getId();
        check(confirmarConcorrente(mesmoId, mesmoId) == 1
                && medicamentos.buscarPorId(concorrente.getId()).getQuantidadeEstoque() == 9,
                "Duas conexoes nao confirmam a mesma dose duas vezes");
        Medicamento ultimo = novo("Concorrencia ultimo comprimido", 1, 12, 1);
        List<Dose> ultimas = doses.listarPorMedicamento(ultimo.getId());
        check(confirmarConcorrente(ultimas.get(0).getId(), ultimas.get(1).getId()) == 1
                && medicamentos.buscarPorId(ultimo.getId()).getQuantidadeEstoque() == 0,
                "Duas doses concorrentes nao tornam estoque negativo");

        String cadastro = console("1\nMedicamento CLI\n12\n12\n1\nS\n0\n");
        Medicamento cli = medicamentos.listarTodos().stream()
                .filter(x -> x.getNome().equals("Medicamento CLI")).findFirst().orElseThrow();
        check(cadastro.contains("Medicamento cadastrado"), "Opcao 1 executa Create pelo Scanner");
        check(console("2\n0\n").contains("Medicamento CLI"), "Opcao 2 executa Read");
        check(console("3\n" + cli.getId() + "\nAtualizado CLI\n\n\nS\n0\n")
                .contains("Medicamento atualizado"), "Opcao 3 executa Update");
        check(console("4\n" + cli.getId() + "\n10\nS\n0\n").contains("Estoque ajustado"),
                "Opcao 4 ajusta estoque");
        int cliDose = doses.listarPorMedicamento(cli.getId()).get(0).getId();
        String confirmacaoCLI = console("6\n" + cliDose + "\nS\n0\n");
        check(confirmacaoCLI.contains("Dose confirmada e estoque baixado")
                && confirmacaoCLI.contains("ALERTA DE RECOMPRA"), "Opcao 6 confirma e mostra alerta");
        check(!console("7\nN\n0\n").contains("HISTORICO RECENTE"),
                "Opcao 7 nao mostra painel sem autorizacao");
        check(console("7\nS\n0\n").contains("HISTORICO RECENTE"),
                "Opcao 7 mostra painel autorizado");
        check(console("5\n" + cli.getId() + "\nN\n0\n").contains("Operacao cancelada")
                && medicamentos.buscarPorId(cli.getId()) != null, "Cancelamento nao exclui");
        check(console("5\n" + cli.getId() + "\nS\n0\n").contains("Medicamento e doses excluidos")
                && medicamentos.buscarPorId(cli.getId()) == null
                && doses.listarPorMedicamento(cli.getId()).isEmpty(), "Opcao 5 exclui medicamento e doses");
        check(console("abc\n99\n0\n").contains("Digite um numero inteiro"), "Entrada invalida retorna ao menu");
        int totalAntesEOF = medicamentos.listarTodos().size();
        check(console("1\nCadastro interrompido\n").contains("Entrada encerrada")
                && medicamentos.listarTodos().size() == totalAntesEOF, "EOF nao cria dados artificiais");

        medicamentos.excluir(idAtual);
        check(medicamentos.buscarPorId(idAtual) == null && doses.listarPorMedicamento(idAtual).isEmpty(),
                "Delete remove entidade e historico no banco");
        System.out.println("RESULTADO: " + total + " verificacoes passaram com SQLite real.");
    }

    private static Medicamento novo(String nome, int estoque, int intervalo, int quantidade) throws SQLException {
        Medicamento m = new Medicamento(1, nome, estoque, intervalo, quantidade);
        doses.cadastrarTratamento(m, 4);
        return m;
    }

    private static int confirmarConcorrente(int a, int b) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int id : new int[]{a, b}) {
                resultados.add(pool.submit(() -> {
                    iniciar.await();
                    try { new DoseDAO().confirmarTomadaDose(id, 1); return true; }
                    catch (SQLException | IllegalStateException e) { return false; }
                }));
            }
            iniciar.countDown();
            int sucesso = 0;
            for (Future<Boolean> resultado : resultados) if (resultado.get(15, TimeUnit.SECONDS)) sucesso++;
            return sucesso;
        } finally { pool.shutdownNow(); }
    }

    private static String console(String entrada) throws Exception {
        ByteArrayOutputStream captura = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream saida = new PrintStream(captura, true, StandardCharsets.UTF_8)) {
            System.setOut(saida);
            new ConsoleView(new Scanner(entrada)).iniciar();
        } finally { System.setOut(original); }
        return captura.toString(StandardCharsets.UTF_8);
    }

    private static void executarSQL(String sql) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    @FunctionalInterface private interface Acao { void executar() throws Exception; }

    private static void falha(Acao acao, String descricao) throws Exception {
        try { acao.executar(); } catch (SQLException | IllegalArgumentException | IllegalStateException esperado) {
            check(true, descricao);
            return;
        }
        throw new AssertionError("Era esperada falha: " + descricao);
    }

    private static void check(boolean condicao, String descricao) {
        if (!condicao) throw new AssertionError(descricao);
        total++;
        System.out.println("OK " + total + " - " + descricao);
    }
}
