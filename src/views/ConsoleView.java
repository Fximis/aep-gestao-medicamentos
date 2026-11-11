package views;

import dao.DoseDAO;
import dao.GenericDAO;
import dao.MedicamentoDAO;
import dao.UsuarioDAO;
import models.Dose;
import models.Medicamento;
import models.Paciente;
import models.Responsavel;
import models.Usuario;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;

/**
 * Console de Carlos Henrique: CRUD, baixa de doses e acompanhamento familiar.
 * Usa os modelos e o contrato GenericDAO do grupo, sem duplicar regras de autonomia.
 */
public class ConsoleView {
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final Scanner scanner;
    private final MedicamentoDAO medicamentoDAO = new MedicamentoDAO();
    private final GenericDAO<Medicamento> medicamentos = medicamentoDAO;
    private final DoseDAO doses = new DoseDAO();
    private final UsuarioDAO usuarios = new UsuarioDAO();

    public ConsoleView() {
        this(new Scanner(System.in));
    }

    public ConsoleView(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner);
    }

    /** Tambem permite executar diretamente: java ... views.ConsoleView */
    public static void main(String[] args) {
        new ConsoleView().iniciar();
    }

    public void iniciar() {
        try {
            List<Paciente> pacientes = usuarios.listarPacientes();
            if (pacientes.isEmpty()) {
                erro("Nenhum paciente cadastrado. Execute a carga inicial do projeto.");
                return;
            }
            // Sessao local de demonstracao: usa um paciente real do banco, nunca um objeto ficticio.
            Paciente paciente = pacientes.get(0);
            if (pacientes.size() > 1) {
                System.out.println("Selecione o paciente da demonstracao:");
                for (int i = 0; i < pacientes.size(); i++)
                    System.out.printf("[%d] %s%n", i + 1, pacientes.get(i).getNome());
                paciente = pacientes.get(lerInteiro("Paciente: ", 1, pacientes.size()) - 1);
            }
            Responsavel responsavel = usuarios.listarTodos().stream()
                    .filter(Responsavel.class::isInstance).map(Responsavel.class::cast)
                    .findFirst().orElse(null);
            executar(paciente, responsavel);
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada encerrada. Nenhuma operacao incompleta foi confirmada.");
        } catch (SQLException e) {
            erro("Nao foi possivel abrir a sessao no banco: " + e.getMessage());
        }
    }

    public void executar(Paciente paciente, Responsavel responsavel) {
        try {
            while (true) {
                System.out.println("\n=== GESTAO DE MEDICAMENTOS ===");
                System.out.println("Paciente: " + paciente.getNome());
                try {
                    long pendentes = doses.listarPendentesPorPaciente(paciente.getId()).stream()
                            .filter(d -> !d.getHorarioPrevisto().isAfter(LocalDateTime.now())).count();
                    System.out.println("Lembrete: " + pendentes + " dose(s) pendente(s) com horario atingido.");
                    for (Medicamento m : listarDoPaciente(paciente.getId()))
                        if (m.verificarAlertaRecompra()) exibirAlerta(m);
                    int opcao = exibirMenuPrincipal();
                    if (opcao == 0) {
                        System.out.println("Aplicacao encerrada.");
                        return;
                    }
                    switch (opcao) {
                        case 1 -> cadastrar(paciente.getId());
                        case 2 -> exibirMedicamentos(listarDoPaciente(paciente.getId()));
                        case 3 -> atualizar(paciente.getId());
                        case 4 -> ajustarEstoque(paciente.getId());
                        case 5 -> excluir(paciente.getId());
                        case 6 -> confirmarDose(paciente.getId());
                        case 7 -> exibirCuidador(paciente, responsavel);
                        default -> erro("Opcao invalida.");
                    }
                } catch (SQLException e) {
                    erro("Operacao nao concluida no banco: " + e.getMessage());
                } catch (IllegalArgumentException | IllegalStateException e) {
                    erro(e.getMessage());
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada encerrada. Retorne ao programa para continuar.");
        }
    }

    public int exibirMenuPrincipal() {
        System.out.println("[1] Cadastrar Medicamento (Create)");
        System.out.println("[2] Listar Medicamentos e Autonomia (Read)");
        System.out.println("[3] Atualizar Medicamento (Update)");
        System.out.println("[4] Ajustar Estoque Manualmente (Update de Estoque)");
        System.out.println("[5] Excluir Medicamento (Delete)");
        System.out.println("[6] Confirmar Dose Tomada (Baixa + Alerta se <= 5 dias)");
        System.out.println("[7] Visao do Responsavel / Cuidador");
        System.out.println("[0] Sair");
        return lerInteiro("Opcao: ", 0, 7);
    }

    private void cadastrar(int pacienteId) throws SQLException {
        String nome = lerNome("Nome do medicamento: ");
        int estoque = lerInteiro("Estoque inicial: ", 0, Integer.MAX_VALUE);
        int frequencia = lerInteiro("Intervalo entre doses (horas): ", 1, Integer.MAX_VALUE);
        int porDose = lerInteiro("Quantidade por dose: ", 1, Integer.MAX_VALUE);
        Medicamento novo = new Medicamento(pacienteId, nome, estoque, frequencia, porDose);
        if (!confirmar("Salvar medicamento e agendar as primeiras 4 doses?")) return;
        doses.cadastrarTratamento(novo, 4);
        sucesso("Medicamento cadastrado. ID: " + novo.getId());
        exibirMedicamentos(List.of(novo));
        System.out.println("Agenda inicial criada a partir do horario atual, sem baixar o estoque.");
    }

    private void atualizar(int pacienteId) throws SQLException {
        Medicamento m = selecionarMedicamento(pacienteId);
        if (m == null) return;
        System.out.println("ENTER mantem o valor atual. O estoque e ajustado na opcao 4.");
        String nome;
        do {
            nome = lerTexto("Nome [" + m.getNome() + "]: ");
            if (nome.length() <= 100) break;
            erro("Use ate 100 caracteres.");
        } while (true);
        int frequencia = lerInteiroComPadrao("Intervalo em horas [" + m.getFrequenciaHoras() + "]: ",
                m.getFrequenciaHoras());
        int porDose = lerInteiroComPadrao("Quantidade por dose [" + m.getQuantidadePorDose() + "]: ",
                m.getQuantidadePorDose());
        boolean mudouPosologia = frequencia != m.getFrequenciaHoras() || porDose != m.getQuantidadePorDose();
        if (mudouPosologia)
            System.out.println("As doses pendentes serao reagendadas. Proxima dose: agora + novo intervalo.");
        if (!confirmar("Salvar alteracoes?")) return;
        if (!nome.isBlank()) m.setNome(nome);
        m.setFrequenciaHoras(frequencia);
        m.setQuantidadePorDose(porDose);
        // A agenda so e refeita quando a posologia muda; alteracoes de nome preservam horarios.
        if (mudouPosologia) {
            doses.atualizarTratamento(m, 4);
        } else {
            // Atualiza apenas o nome, sem sobrescrever estoque que possa ter mudado.
            doses.atualizarNome(m);
        }
        sucesso("Medicamento atualizado.");
        exibirMedicamentos(List.of(medicamentos.buscarPorId(m.getId())));
    }

    private void ajustarEstoque(int pacienteId) throws SQLException {
        Medicamento m = selecionarMedicamento(pacienteId);
        if (m == null) return;
        int novo = lerInteiro("Novo total fisico em estoque (substitui o anterior): ", 0, Integer.MAX_VALUE);
        m.setQuantidadeEstoque(novo); // A validacao RN04 permanece no modelo do Lucas.
        if (!confirmar("Confirmar estoque de " + novo + " unidade(s)?")) return;
        medicamentoDAO.atualizarEstoque(m.getId(), novo);
        sucesso("Estoque ajustado.");
        exibirMedicamentos(List.of(medicamentos.buscarPorId(m.getId())));
    }

    private void excluir(int pacienteId) throws SQLException {
        Medicamento m = selecionarMedicamento(pacienteId);
        if (m == null) return;
        if (!confirmar("Excluir '" + m.getNome() + "' e TODO o historico de doses?")) return;
        medicamentos.excluir(m.getId());
        sucesso("Medicamento e doses excluidos.");
    }

    private void confirmarDose(int pacienteId) throws SQLException {
        List<Dose> pendentes = doses.listarPendentesPorPaciente(pacienteId);
        if (pendentes.isEmpty()) {
            System.out.println("Nenhuma dose pendente.");
            return;
        }
        System.out.println("\nDOSES PENDENTES (ordenadas por horario)");
        for (Dose d : pendentes)
            System.out.printf("ID %d | %s | Previsto: %s | PENDENTE%n",
                    d.getId(), d.getNomeMedicamento(), d.getHorarioPrevisto().format(DATA));
        int id = lerInteiro("ID da dose (0 cancela): ", 0, Integer.MAX_VALUE);
        if (id == 0) return;
        Dose escolhida = pendentes.stream().filter(d -> d.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Escolha uma dose pendente deste paciente."));
        if (escolhida.getHorarioPrevisto().isAfter(LocalDateTime.now()))
            System.out.println("Este registro tem horario previsto no futuro.");
        if (!confirmar("Voce confirma que JA TOMOU esta dose?")) return;
        Medicamento atualizado = doses.confirmarTomadaDose(id, pacienteId);
        sucesso("Dose confirmada e estoque baixado.");
        exibirMedicamentos(List.of(atualizado));
    }

    private void exibirCuidador(Paciente paciente, Responsavel responsavel) throws SQLException {
        if (responsavel == null) {
            System.out.println("Nenhum responsavel cadastrado.");
            return;
        }
        // RN08: consentimento explicito para esta visualizacao local. Nao simula login/permissoes persistidas.
        if (!confirmar("Paciente " + paciente.getNome() + " autoriza " +
                responsavel.getNome() + " a visualizar os dados nesta sessao?")) return;
        System.out.println("\nCUIDADOR: " + responsavel.getNome() + " | " + responsavel.getVinculoFamiliar());
        exibirMedicamentos(listarDoPaciente(paciente.getId()));
        System.out.println("DOSES PENDENTES:");
        for (Dose d : doses.listarPendentesPorPaciente(paciente.getId()))
            System.out.printf("ID %d | %s | %s | PENDENTE%n", d.getId(),
                    d.getNomeMedicamento(), d.getHorarioPrevisto().format(DATA));
        System.out.println("HISTORICO RECENTE (ate 20 doses, horario REAL de consumo):");
        for (Dose d : doses.listarHistoricoRecente(paciente.getId(), 20))
            System.out.printf("ID %d | %s | Consumida: %s%n", d.getId(),
                    d.getNomeMedicamento(), d.getHorarioConsumo().format(DATA));
        System.out.println("Alertas disponiveis neste console. Nao ha envio de SMS ou mensagens externas.");
    }

    private List<Medicamento> listarDoPaciente(int pacienteId) throws SQLException {
        return medicamentoDAO.listarPorUsuario(pacienteId);
    }

    private Medicamento selecionarMedicamento(int pacienteId) throws SQLException {
        List<Medicamento> lista = listarDoPaciente(pacienteId);
        exibirMedicamentos(lista);
        if (lista.isEmpty()) return null;
        int id = lerInteiro("ID do medicamento (0 cancela): ", 0, Integer.MAX_VALUE);
        if (id == 0) return null;
        Medicamento m = medicamentos.buscarPorId(id);
        if (m == null || m.getUsuarioId() != pacienteId)
            throw new IllegalArgumentException("Medicamento nao encontrado para este paciente.");
        return m;
    }

    private void exibirMedicamentos(List<Medicamento> lista) {
        if (lista.isEmpty()) System.out.println("Nenhum medicamento cadastrado.");
        for (Medicamento m : lista) {
            System.out.printf("ID %d | %s | Estoque: %d | %d unidade(s)/dose a cada %dh | Autonomia: %d dia(s)%n",
                    m.getId(), m.getNome(), m.getQuantidadeEstoque(), m.getQuantidadePorDose(),
                    m.getFrequenciaHoras(), m.calcularDiasRestantes());
            if (m.verificarAlertaRecompra()) exibirAlerta(m);
        }
    }

    private void exibirAlerta(Medicamento m) {
        System.out.printf("[ALERTA DE RECOMPRA] %s: autonomia de %d dia(s), limite de 5 dias.%n",
                m.getNome(), m.calcularDiasRestantes());
    }

    private String lerTexto(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) throw new NoSuchElementException("Fim da entrada.");
        return scanner.nextLine().trim();
    }

    private String lerNome(String prompt) {
        while (true) {
            String nome = lerTexto(prompt);
            if (!nome.isBlank() && nome.length() <= 100) return nome;
            erro("Informe um nome de 1 a 100 caracteres.");
        }
    }

    private int lerInteiro(String prompt, int minimo, int maximo) {
        while (true) {
            try {
                int valor = Integer.parseInt(lerTexto(prompt));
                if (valor >= minimo && valor <= maximo) return valor;
            } catch (NumberFormatException ignored) {
                // Repete a leitura; nao consome silenciosamente entradas invalidas.
            }
            erro("Digite um numero inteiro entre " + minimo + " e " + maximo + ".");
        }
    }

    private int lerInteiroComPadrao(String prompt, int padrao) {
        while (true) {
            String texto = lerTexto(prompt);
            if (texto.isEmpty()) return padrao;
            try {
                int valor = Integer.parseInt(texto);
                if (valor > 0) return valor;
            } catch (NumberFormatException ignored) {
            }
            erro("Informe um inteiro positivo ou ENTER.");
        }
    }

    private boolean confirmar(String mensagem) {
        while (true) {
            String resposta = lerTexto(mensagem + " (S/N): ");
            if ("S".equalsIgnoreCase(resposta) || "SIM".equalsIgnoreCase(resposta)) return true;
            if ("N".equalsIgnoreCase(resposta) || "NAO".equalsIgnoreCase(resposta)
                    || "NÃO".equalsIgnoreCase(resposta)) {
                System.out.println("Operacao cancelada.");
                return false;
            }
            erro("Responda S ou N.");
        }
    }

    private void sucesso(String mensagem) { System.out.println("[SUCESSO] " + mensagem); }
    private void erro(String mensagem) { System.out.println("[ERRO] " + mensagem); }
}
