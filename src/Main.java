import config.DatabaseConnection;
import dao.DoseDAO;
import dao.MedicamentoDAO;
import dao.UsuarioDAO;
import models.Dose;
import models.Medicamento;
import models.Paciente;
import models.Responsavel;
import models.Usuario;
import views.ConsoleView;

import java.sql.SQLException;
import java.util.List;

/**
 * Ponto de Entrada Principal (Bootstrap & Controller) da Aplicação.
 * Orquestra as regras de negócio, persistência via DAO e visualização no console,
 * proporcionando uma experiência acolhedora para o paciente e rigor técnico para a avaliação.
 */
public class Main {

    private static final ConsoleView view = new ConsoleView();
    private static final MedicamentoDAO medicamentoDAO = new MedicamentoDAO();
    private static final DoseDAO doseDAO = new DoseDAO();
    private static final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public static void main(String[] args) {
        // 1. Inicialização e Teste de Conexão com o Banco de Dados
        try {
            DatabaseConnection.getConnection();
        } catch (SQLException e) {
            view.exibirMensagemErro("Não foi possível conectar ao banco de dados: " + e.getMessage());
            return;
        }

        // 2. Carrega Usuário Padrão para a Sessão (Paciente João da Silva e Cuidadora Maria)
        Paciente pacienteAtivo = carregarPacientePadrao();
        Responsavel responsavelPadrao = carregarResponsavelPadrao();

        boolean executando = true;

        while (executando) {
            try {
                // Consulta dados em tempo real para alimentar o Dashboard Proativo do Paciente
                List<Dose> dosesPendentes = doseDAO.listarPendentesPorPaciente(pacienteAtivo.getId());
                List<Medicamento> todosMeds = medicamentoDAO.listarPorUsuario(pacienteAtivo.getId());
                List<Medicamento> criticos = todosMeds.stream()
                        .filter(Medicamento::verificarAlertaRecompra)
                        .toList();

                view.exibirCabecalhoHumano(pacienteAtivo, responsavelPadrao);
                view.exibirResumoDoDia(dosesPendentes, criticos);
                int opcao = view.exibirMenuPrincipal();

                switch (opcao) {
                    case 1 -> tomarMedicamento(pacienteAtivo, responsavelPadrao);
                    case 2 -> verMinhaRotina(pacienteAtivo);
                    case 3 -> verMeusMedicamentos(pacienteAtivo);
                    case 4 -> reporEstoque(pacienteAtivo, responsavelPadrao);
                    case 5 -> cadastrarNovoTratamento(pacienteAtivo);
                    case 6 -> gerenciarMedicamentos(pacienteAtivo);
                    case 7 -> espacoDaFamilia(responsavelPadrao, pacienteAtivo);
                    case 8 -> painelTecnicoAEP();
                    case 0 -> {
                        System.out.println("\nCuidado Diário finalizado com sucesso. Tenha um ótimo dia e cuide da sua saúde!");
                        DatabaseConnection.closeConnection();
                        executando = false;
                    }
                    default -> view.exibirMensagemErro("Opção não reconhecida. Por favor, escolha um número válido do menu.");
                }

                if (executando) {
                    view.aguardarEnter();
                }

            } catch (Exception e) {
                view.exibirMensagemErro("Ocorreu um imprevisto na operação: " + e.getMessage());
                view.aguardarEnter();
            }
        }
    }

    /**
     * [Opção 1] Fluxo guiado para o paciente confirmar a ingestão da dose.
     * Lista amigavelmente apenas os remédios pendentes e dá baixa atômica no estoque.
     */
    private static void tomarMedicamento(Paciente paciente, Responsavel responsavel) throws SQLException {
        List<Dose> dosesPendentes = doseDAO.listarPendentesPorPaciente(paciente.getId());

        if (dosesPendentes.isEmpty()) {
            view.exibirMensagemSucesso("Que maravilha! Você não tem nenhuma dose pendente para tomar agora.");
            return;
        }

        view.exibirListaMedicamentosParaTomar(dosesPendentes);
        int escolha = view.lerInteiro("Digite o número do remédio [1-" + dosesPendentes.size() + "] ou 0 para voltar: ");

        if (escolha == 0) {
            System.out.println("Operação cancelada. Nenhuma alteração foi realizada.");
            return;
        }

        if (escolha < 1 || escolha > dosesPendentes.size()) {
            view.exibirMensagemErro("Opção inválida. Escolha um número entre 1 e " + dosesPendentes.size() + ".");
            return;
        }

        Dose doseEscolhida = dosesPendentes.get(escolha - 1);

        try {
            Medicamento medAtualizado = doseDAO.confirmarTomadaDose(doseEscolhida.getId());
            view.exibirConfirmacaoDoseSucesso(medAtualizado, responsavel);
        } catch (IllegalStateException | IllegalArgumentException e) {
            view.exibirMensagemErro(e.getMessage());
        }
    }

    /**
     * [Opção 2] Exibe a agenda de remédios de hoje e o histórico recente.
     */
    private static void verMinhaRotina(Paciente paciente) throws SQLException {
        List<Dose> pendentes = doseDAO.listarPendentesPorPaciente(paciente.getId());
        List<Dose> historico = doseDAO.listarHistoricoRecente(paciente.getId(), 5);
        view.exibirMinhaRotina(pendentes, historico);
    }

    /**
     * [Opção 3] Consulta o estoque atual e cálculo dinâmico de autonomia de cada remédio.
     */
    private static void verMeusMedicamentos(Paciente paciente) throws SQLException {
        List<Medicamento> medicamentos = medicamentoDAO.listarPorUsuario(paciente.getId());
        view.exibirCatalogoMedicamentos(medicamentos);
    }

    /**
     * [Opção 4] Reabastecimento intuitivo de estoque (Chegou caixinha nova).
     * Permite adicionar comprimidos de nova embalagem ou retificar contagem física (RF07 / RN07).
     */
    private static void reporEstoque(Paciente paciente, Responsavel responsavel) throws SQLException {
        List<Medicamento> medicamentos = medicamentoDAO.listarPorUsuario(paciente.getId());

        if (medicamentos.isEmpty()) {
            view.exibirMensagemAviso("Você ainda não possui medicamentos cadastrados para repor estoque.");
            return;
        }

        System.out.println("\n" + "-".repeat(78));
        System.out.println("                 📦 REPOR ESTOQUE (CHEGOU CAIXINHA NOVA!)");
        System.out.println("-".repeat(78));
        System.out.println("Selecione qual medicamento você deseja abastecer:\n");

        for (int i = 0; i < medicamentos.size(); i++) {
            Medicamento m = medicamentos.get(i);
            System.out.printf("  [%d] %-28s (Estoque atual: %3d comp. | Dura %2d dias)\n",
                    (i + 1), m.getNome(), m.getQuantidadeEstoque(), m.calcularDiasRestantes());
        }
        System.out.println("\n  [0] Voltar ao menu principal");
        System.out.println("-".repeat(78));

        int escolha = view.lerInteiro("Escolha o medicamento [1-" + medicamentos.size() + "] ou 0: ");
        if (escolha == 0) {
            return;
        }
        if (escolha < 1 || escolha > medicamentos.size()) {
            view.exibirMensagemErro("Medicamento inválido.");
            return;
        }

        Medicamento med = medicamentos.get(escolha - 1);

        System.out.println("\nComo você prefere atualizar o estoque de '" + med.getNome() + "'?");
        System.out.println("  [1] Adicionar comprimidos de uma nova caixa comprada (Ex: comprei caixa com 30)");
        System.out.println("  [2] Digitar a contagem total exata (conferência do armário)");
        System.out.println("  [0] Cancelar");
        int modo = view.lerInteiro("Escolha a opção: ");

        if (modo == 0) {
            return;
        }

        int estoqueAnterior = med.getQuantidadeEstoque();
        int novoEstoque;

        if (modo == 1) {
            int comprados = view.lerInteiro("Quantos comprimidos vieram na embalagem nova? ");
            if (comprados <= 0) {
                view.exibirMensagemErro("A quantidade comprada deve ser maior que zero.");
                return;
            }
            med.adicionarEstoque(comprados);
            novoEstoque = med.getQuantidadeEstoque();
            medicamentoDAO.atualizarEstoque(med.getId(), novoEstoque);

            System.out.println("\n✔ Estoque reabastecido com sucesso!");
            System.out.printf("  Medicamento: %s\n", med.getNome());
            System.out.printf("  Estoque anterior: %d comp. ➔ Novo estoque: %d comp. (+%d comp.)\n",
                    estoqueAnterior, novoEstoque, comprados);
        } else if (modo == 2) {
            novoEstoque = view.lerInteiro("Informe o total exato de comprimidos contados: ");
            if (novoEstoque < 0) {
                view.exibirMensagemErro("O estoque físico não pode ser negativo (RN04).");
                return;
            }
            med.setQuantidadeEstoque(novoEstoque);
            medicamentoDAO.atualizarEstoque(med.getId(), novoEstoque);

            System.out.println("\n✔ Contagem de estoque atualizada com sucesso!");
            System.out.printf("  Medicamento: %s\n", med.getNome());
            System.out.printf("  Novo estoque físico conferido: %d comprimidos.\n", novoEstoque);
        } else {
            view.exibirMensagemErro("Opção não reconhecida.");
            return;
        }

        int novaAutonomia = med.calcularDiasRestantes();
        System.out.printf("  Nova autonomia: Dura mais %d dias de tratamento!\n", novaAutonomia);
        if (med.verificarAlertaRecompra()) {
            System.out.printf("  ⚠️ Aviso: Mesmo com o ajuste, a autonomia é de %d dias (menor ou igual a 5 dias).\n", novaAutonomia);
        } else {
            System.out.println("  Situação: ✅ Nível Seguro. Não precisa se preocupar com falta por enquanto.");
        }
    }

    /**
     * [Opção 5] Cadastro de novo medicamento com linguagem acolhedora (Create do CRUD).
     */
    private static void cadastrarNovoTratamento(Paciente paciente) throws SQLException {
        System.out.println("\n" + "-".repeat(78));
        System.out.println("               ➕ CADASTRAR NOVO MEDICAMENTO / TRATAMENTO");
        System.out.println("-".repeat(78));
        System.out.println("Preencha os dados da receita médica com atenção:\n");

        String nome = view.lerTexto("• Nome do medicamento e dosagem (ex: Dipirona 500mg, Losartana 50mg): ");
        if (nome.isEmpty()) {
            view.exibirMensagemErro("O nome do medicamento não pode ficar em branco.");
            return;
        }

        int estoqueInicial = view.lerInteiro("• Quantos comprimidos você tem em casa agora? (ex: 30): ");
        if (estoqueInicial < 0) {
            view.exibirMensagemErro("A quantidade inicial não pode ser negativa.");
            return;
        }

        int frequenciaHoras = view.lerInteiro("• De quantas em quantas horas deve tomar? (ex: 8 para 3x/dia, 12 para 2x/dia, 24 para 1x/dia): ");
        if (frequenciaHoras <= 0) {
            view.exibirMensagemErro("A frequência deve ser de pelo menos 1 hora.");
            return;
        }

        int qtdPorDose = view.lerInteiroComPadrao("• Quantos comprimidos toma por vez? [Pressione ENTER para 1]: ", 1);
        if (qtdPorDose <= 0) {
            view.exibirMensagemErro("A quantidade por dose deve ser de pelo menos 1 comprimido.");
            return;
        }

        Medicamento novo = new Medicamento(paciente.getId(), nome, estoqueInicial, frequenciaHoras, qtdPorDose);
        medicamentoDAO.inserir(novo);

        // Gera automaticamente a primeira agenda de doses do tratamento
        doseDAO.gerarAgendaFutura(novo, 4);

        System.out.println("\n" + "=".repeat(78));
        System.out.printf("✔ O tratamento com '%s' foi cadastrado com carinho no sistema!\n", novo.getNome());
        System.out.printf("  Autonomia calculada: %d dias de medicação garantida.\n", novo.calcularDiasRestantes());
        System.out.println("  Suas próximas doses já foram automaticamente organizadas na sua rotina.");
        if (novo.verificarAlertaRecompra()) {
            System.out.println("  🚨 Atenção: Este medicamento já começa com autonomia menor ou igual a 5 dias!");
        }
        System.out.println("=".repeat(78));
    }

    /**
     * [Opção 6] Gestão de Medicamentos (Alterar dados posológicos ou encerrar tratamento).
     * Realiza o Update e Delete do CRUD de forma segura e humanizada.
     */
    private static void gerenciarMedicamentos(Paciente paciente) throws SQLException {
        List<Medicamento> medicamentos = medicamentoDAO.listarPorUsuario(paciente.getId());

        if (medicamentos.isEmpty()) {
            view.exibirMensagemAviso("Você não possui medicamentos cadastrados para gerenciar.");
            return;
        }

        System.out.println("\n" + "-".repeat(78));
        System.out.println("                     ⚙️ GERENCIAR MEDICAMENTOS");
        System.out.println("-".repeat(78));
        System.out.println("  [1] Alterar dados de um medicamento (nome, frequência de horários ou posologia)");
        System.out.println("  [2] Encerrar / Excluir medicamento do tratamento (Alta médica)");
        System.out.println("  [0] Voltar ao menu principal");
        System.out.println("-".repeat(78));

        int acao = view.lerInteiro("Escolha o que deseja fazer: ");
        if (acao == 0) {
            return;
        }

        if (acao == 1) {
            // Update do CRUD
            System.out.println("\nSelecione qual medicamento deseja atualizar:");
            for (int i = 0; i < medicamentos.size(); i++) {
                Medicamento m = medicamentos.get(i);
                System.out.printf("  [%d] %-28s (De %dh em %dh | %d comp. por dose)\n",
                        (i + 1), m.getNome(), m.getFrequenciaHoras(), m.getFrequenciaHoras(), m.getQuantidadePorDose());
            }
            int esc = view.lerInteiro("Escolha o medicamento [1-" + medicamentos.size() + "]: ");
            if (esc < 1 || esc > medicamentos.size()) {
                view.exibirMensagemErro("Medicamento inválido.");
                return;
            }

            Medicamento med = medicamentos.get(esc - 1);

            System.out.println("\nAtualização de dados de '" + med.getNome() + "' (ENTER mantém o valor atual):");
            String novoNome = view.lerTexto("Novo nome comercial [" + med.getNome() + "]: ");
            if (!novoNome.isEmpty()) {
                med.setNome(novoNome);
            }

            int novaFreq = view.lerInteiroComPadrao("Nova frequência em horas [" + med.getFrequenciaHoras() + "h]: ", med.getFrequenciaHoras());
            med.setFrequenciaHoras(novaFreq);

            int novaQtdDose = view.lerInteiroComPadrao("Nova quantidade por dose [" + med.getQuantidadePorDose() + "]: ", med.getQuantidadePorDose());
            med.setQuantidadePorDose(novaQtdDose);

            medicamentoDAO.atualizar(med);
            view.exibirMensagemSucesso("Dados do medicamento atualizados com sucesso!");
            System.out.printf("Nova autonomia calculada: %d dias de tratamento.\n", med.calcularDiasRestantes());

        } else if (acao == 2) {
            // Delete do CRUD
            System.out.println("\nSelecione qual medicamento deseja remover do tratamento:");
            for (int i = 0; i < medicamentos.size(); i++) {
                Medicamento m = medicamentos.get(i);
                System.out.printf("  [%d] %s\n", (i + 1), m.getNome());
            }
            int esc = view.lerInteiro("Escolha o medicamento [1-" + medicamentos.size() + "]: ");
            if (esc < 1 || esc > medicamentos.size()) {
                view.exibirMensagemErro("Medicamento inválido.");
                return;
            }

            Medicamento med = medicamentos.get(esc - 1);
            String confirmacao = view.lerTexto("Tem certeza de que o tratamento com '" + med.getNome() + "' foi encerrado e deseja removê-lo? (S/N): ");
            if ("S".equalsIgnoreCase(confirmacao)) {
                medicamentoDAO.excluir(med.getId());
                view.exibirMensagemSucesso("Medicamento e histórico de doses removidos com sucesso.");
            } else {
                System.out.println("Operação cancelada. Medicamento mantido ativo.");
            }
        } else {
            view.exibirMensagemErro("Opção não reconhecida.");
        }
    }

    /**
     * [Opção 7] Painel do Responsável/Cuidador (RF06 / RN08).
     */
    private static void espacoDaFamilia(Responsavel responsavel, Paciente paciente) throws SQLException {
        List<Medicamento> medicamentos = medicamentoDAO.listarPorUsuario(paciente.getId());
        List<Dose> dosesPendentes = doseDAO.listarPendentesPorPaciente(paciente.getId());
        view.exibirPainelCuidador(responsavel, paciente, medicamentos, dosesPendentes);
    }

    /**
     * [Opção 8] Demonstração Técnica e Diretrizes da AEP (Banca Avaliadora).
     * Comprova formalmente POO, JDBC, CRUD e Aderência ao Planejamento.
     */
    private static void painelTecnicoAEP() throws SQLException {
        List<Usuario> usuarios = usuarioDAO.listarTodos();
        boolean isSQLite = DatabaseConnection.isUsingSQLite();
        view.exibirPainelTecnicoAEP(usuarios, isSQLite);
    }

    private static Paciente carregarPacientePadrao() {
        try {
            List<Paciente> pacientes = usuarioDAO.listarPacientes();
            if (!pacientes.isEmpty()) {
                return pacientes.get(0);
            }
        } catch (SQLException ignored) {
        }
        return new Paciente(1, "João da Silva", "joao@email.com", "123456", "Hipertensão e Diabetes Tipo 2");
    }

    private static Responsavel carregarResponsavelPadrao() {
        try {
            Usuario u = usuarioDAO.buscarPorId(2);
            if (u instanceof Responsavel r) {
                return r;
            }
        } catch (SQLException ignored) {
        }
        return new Responsavel(2, "Maria Silva Santos", "maria@email.com", "123456", "Filha e Cuidadora");
    }
}
