package views;

import dao.DoseDAO;
import auth.Autenticacao;
import dao.FamiliarDAO;
import dao.MedicamentoDAO;
import models.Dose;
import models.Medicamento;
import models.Paciente;
import models.Usuario;

import java.io.Console;
import java.sql.SQLException;
import java.util.Arrays;
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
    private final Console terminal;
    private final boolean entradaInjetada;
    private final MedicamentoDAO medicamentoDAO = new MedicamentoDAO();
    private final DoseDAO doses = new DoseDAO();
    private final Autenticacao autenticacao = new Autenticacao();
    private final FamiliarDAO familia = new FamiliarDAO();

    public ConsoleView() {
        terminal = System.console();
        scanner = terminal == null ? new Scanner(System.in) : null;
        entradaInjetada = false;
    }

    public ConsoleView(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner);
        terminal = null;
        entradaInjetada = true;
    }

    /** Tambem permite executar diretamente: java ... views.ConsoleView */
    public static void main(String[] args) {
        new ConsoleView().iniciar();
    }

    public void iniciar() {
        try {
            while (true) {
                System.out.println("\n=== CUIDADO DIÁRIO ===");
                System.out.println("[1] Entrar  [2] Criar conta  [0] Encerrar aplicativo");
                int opcao = lerInteiro("Opção: ", 0, 2);
                if (opcao == 0) return;
                try {
                    if (opcao == 2) cadastrarConta();
                    else entrar();
                } catch (IllegalArgumentException | IllegalStateException e) {
                    erro(e.getMessage());
                } catch (SQLException e) {
                    erro("Não foi possível acessar o banco. Tente novamente.");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada encerrada. Nenhuma operação incompleta foi confirmada.");
        }
    }

    private void entrar() throws SQLException {
        String email = lerTexto("E-mail: ");
        char[] senha = lerSenha("Senha: ");
        Autenticacao.Sessao sessao;
        try {
            sessao = autenticacao.entrar(email, senha);
        } finally {
            Arrays.fill(senha, '\0');
        }
        try {
            if (sessao.usuario() instanceof Paciente paciente) executarPaciente(sessao, paciente);
            else executarResponsavel(sessao);
        } finally {
            sessao.encerrar();
        }
    }

    private void cadastrarConta() throws SQLException {
        String nome = lerNome("Nome completo: ");
        String email = lerTexto("E-mail: ");
        int opcao = lerInteiro("Perfil [1] Paciente [2] Responsável: ", 1, 2);
        Autenticacao.Perfil perfil = opcao == 1 ? Autenticacao.Perfil.PACIENTE : Autenticacao.Perfil.RESPONSAVEL;
        String detalhe = lerTexto(opcao == 1 ? "Condição médica (opcional): " : "Vínculo familiar (opcional): ");
        char[] senha = lerSenha("Senha (" + Autenticacao.FAIXA_SENHA + "): ");
        char[] confirmacao = new char[0];
        try {
            confirmacao = lerSenha("Confirme a senha: ");
            autenticacao.cadastrar(nome, email, perfil, detalhe, senha, confirmacao);
            sucesso("Conta criada. Entre com seu e-mail e senha.");
        } finally {
            Arrays.fill(senha, '\0');
            Arrays.fill(confirmacao, '\0');
        }
    }

    private void executarPaciente(Autenticacao.Sessao sessao, Paciente paciente) {
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
                if (opcao == 0) return;
                switch (opcao) {
                    case 1 -> cadastrar(paciente.getId());
                    case 2 -> exibirMedicamentos(listarDoPaciente(paciente.getId()));
                    case 3 -> atualizar(paciente.getId());
                    case 4 -> ajustarEstoque(paciente.getId());
                    case 5 -> excluir(paciente.getId());
                    case 6 -> confirmarDose(paciente.getId());
                    case 7 -> gerenciarAcessos(sessao);
                    case 8 -> alterarSenha(sessao);
                    default -> erro("Opcao invalida.");
                }
            } catch (SQLException e) {
                erro("Operação não concluída no banco.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                erro(e.getMessage());
            }
        }
    }

    private void executarResponsavel(Autenticacao.Sessao sessao) {
        while (true) {
            System.out.println("\n=== ACOMPANHAMENTO FAMILIAR ===");
            System.out.println("Responsável: " + sessao.usuario().getNome());
            int opcao = lerInteiro("[1] Consultar paciente [2] Alterar senha [0] Sair da conta: ", 0, 2);
            if (opcao == 0) return;
            try {
                if (opcao == 1) consultarPaciente(sessao);
                else alterarSenha(sessao);
            } catch (IllegalArgumentException | IllegalStateException e) {
                erro(e.getMessage());
            } catch (SQLException e) {
                erro("Operação não concluída no banco.");
            }
        }
    }

    private void alterarSenha(Autenticacao.Sessao sessao) throws SQLException {
        char[] atual = lerSenha("Senha atual: ");
        char[] nova = new char[0];
        char[] confirmacao = new char[0];
        try {
            nova = lerSenha("Nova senha (" + Autenticacao.FAIXA_SENHA + "): ");
            confirmacao = lerSenha("Confirme a nova senha: ");
            autenticacao.alterarSenha(sessao, atual, nova, confirmacao);
            sucesso("Senha atualizada.");
        } finally {
            Arrays.fill(atual, '\0');
            Arrays.fill(nova, '\0');
            Arrays.fill(confirmacao, '\0');
        }
    }

    public int exibirMenuPrincipal() {
        System.out.println("[1] Cadastrar Medicamento (Create)");
        System.out.println("[2] Listar Medicamentos e Autonomia (Read)");
        System.out.println("[3] Atualizar Medicamento (Update)");
        System.out.println("[4] Ajustar Estoque Manualmente (Update de Estoque)");
        System.out.println("[5] Excluir Medicamento (Delete)");
        System.out.println("[6] Confirmar Dose Tomada (Baixa + Alerta se <= 5 dias)");
        System.out.println("[7] Gerenciar acesso familiar");
        System.out.println("[8] Alterar senha");
        System.out.println("[0] Sair da conta");
        return lerInteiro("Opcao: ", 0, 8);
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
        exibirMedicamentos(List.of(medicamentoDAO.buscarPorId(m.getId(), pacienteId)));
    }

    private void ajustarEstoque(int pacienteId) throws SQLException {
        Medicamento m = selecionarMedicamento(pacienteId);
        if (m == null) return;
        int novo = lerInteiro("Novo total fisico em estoque (substitui o anterior): ", 0, Integer.MAX_VALUE);
        m.setQuantidadeEstoque(novo); // A validacao RN04 permanece no modelo do Lucas.
        if (!confirmar("Confirmar estoque de " + novo + " unidade(s)?")) return;
        medicamentoDAO.atualizarEstoque(m.getId(), novo, pacienteId);
        sucesso("Estoque ajustado.");
        exibirMedicamentos(List.of(medicamentoDAO.buscarPorId(m.getId(), pacienteId)));
    }

    private void excluir(int pacienteId) throws SQLException {
        Medicamento m = selecionarMedicamento(pacienteId);
        if (m == null) return;
        if (!confirmar("Excluir '" + m.getNome() + "' e TODO o historico de doses?")) return;
        medicamentoDAO.excluir(m.getId(), pacienteId);
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

    private void gerenciarAcessos(Autenticacao.Sessao sessao) throws SQLException {
        List<Usuario> autorizados = familia.listarResponsaveis(sessao);
        if (autorizados.isEmpty()) System.out.println("Nenhum responsável autorizado.");
        for (int i = 0; i < autorizados.size(); i++)
            System.out.printf("[%d] %s%n", i + 1, autorizados.get(i));
        int opcao = lerInteiro("[1] Autorizar [2] Revogar [0] Voltar: ", 0, 2);
        if (opcao == 1) {
            String email = lerTexto("E-mail do responsável: ");
            if (confirmar("Autorizar consulta de medicamentos e doses para " + email + "?")) {
                familia.autorizar(sessao, email);
                sucesso("Acesso de leitura concedido.");
            }
        } else if (opcao == 2 && !autorizados.isEmpty()) {
            Usuario alvo = autorizados.get(lerInteiro("Responsável: ", 1, autorizados.size()) - 1);
            familia.revogar(sessao, alvo.getId());
            sucesso("Acesso revogado.");
        }
    }

    private void consultarPaciente(Autenticacao.Sessao sessao) throws SQLException {
        List<Usuario> pacientes = familia.listarPacientes(sessao);
        if (pacientes.isEmpty()) {
            System.out.println("Nenhum paciente autorizou o acompanhamento.");
            return;
        }
        for (int i = 0; i < pacientes.size(); i++)
            System.out.printf("[%d] %s%n", i + 1, pacientes.get(i));
        int escolha = lerInteiro("Paciente (0 volta): ", 0, pacientes.size());
        if (escolha == 0) return;
        FamiliarDAO.Acompanhamento resumo = familia.consultar(sessao, pacientes.get(escolha - 1).getId());
        System.out.println("Paciente: " + resumo.paciente().getNome());
        exibirMedicamentos(resumo.medicamentos());
        System.out.println("HORÁRIOS E HISTÓRICO DE DOSES:");
        for (Dose dose : resumo.doses()) {
            System.out.printf("%s | %s | %s%n", dose.getNomeMedicamento(), dose.getHorarioPrevistoFormatado(),
                    dose.isStatusTomado() ? "Confirmada " + dose.getHorarioConsumoFormatado() : "Pendente");
        }
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
        Medicamento m = medicamentoDAO.buscarPorId(id, pacienteId);
        if (m == null)
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
        if (terminal != null) {
            String texto = terminal.readLine("%s", prompt);
            if (texto == null) throw new NoSuchElementException("Fim da entrada.");
            return texto.strip();
        }
        System.out.print(prompt);
        if (!scanner.hasNextLine()) throw new NoSuchElementException("Fim da entrada.");
        return scanner.nextLine().strip();
    }

    private char[] lerSenha(String prompt) {
        if (terminal != null) {
            char[] senha = terminal.readPassword("%s", prompt);
            if (senha == null) throw new NoSuchElementException("Fim da entrada.");
            return senha;
        }
        if (!entradaInjetada) throw new IllegalStateException(
                "Abra o programa em um terminal interativo para digitar a senha sem exibi-la.");
        System.out.print(prompt);
        if (!scanner.hasNextLine()) throw new NoSuchElementException("Fim da entrada.");
        return scanner.nextLine().toCharArray();
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
