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
import views.gui.MainWindow;

import javax.swing.SwingUtilities;
import views.gui.Tema;
import java.awt.GraphicsEnvironment;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Ponto de Entrada Principal (Bootstrap & Controller) da Aplicacao.
 * Orquestra as regras de negocio, persistencia via DAO e visualizacao no console,
 * proporcionando uma experiencia acolhedora para o paciente e rigor tecnico para a avaliacao da AEP.
 */
public class Main {

    private static final ConsoleView view = new ConsoleView();
    private static final MedicamentoDAO medicamentoDAO = new MedicamentoDAO();
    private static final DoseDAO doseDAO = new DoseDAO();
    private static final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public static void main(String[] args) {
        // 1. Inicializacao e Teste de Conexao com o Banco de Dados
        try {
            DatabaseConnection.getConnection();
        } catch (SQLException e) {
            view.exibirMensagemErro("Nao foi possivel conectar ao banco de dados: " + e.getMessage());
            return;
        }

        // 2. Abre a Interface Grafica Desktop (GUI) por padrao
        boolean modoConsole = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg)) {
                modoConsole = true;
                break;
            }
        }

        if (!modoConsole && !GraphicsEnvironment.isHeadless()) {
            SwingUtilities.invokeLater(() -> {
                Tema.instalar();
                new MainWindow().setVisible(true);
            });
            return;
        }

        // 3. Fallback: Carrega Usuario Padrao para a Sessao Console CLI
        Paciente pacienteAtivo = carregarPacientePadrao();
        Responsavel responsavelPadrao = carregarResponsavelPadrao();

        boolean executando = true;

        while (executando) {
            try {
                // Consulta dados em tempo real para alimentar o Dashboard Proativo do Paciente
                List<Dose> devidasAgora = doseDAO.listarDosesParaTomarAgora(pacienteAtivo.getId());
                List<Dose> proximasHoje = doseDAO.listarProximasHoje(pacienteAtivo.getId());
                List<Medicamento> todosMeds = medicamentoDAO.listarPorUsuario(pacienteAtivo.getId());
                List<Medicamento> criticos = todosMeds.stream()
                        .filter(Medicamento::verificarAlertaRecompra)
                        .toList();

                view.exibirCabecalhoHumano(pacienteAtivo, responsavelPadrao);
                view.exibirResumoDoDia(devidasAgora, proximasHoje, criticos);
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
                        System.out.println("\nCuidado Diario finalizado com sucesso. Tenha um otimo dia e cuide da sua saude!");
                        DatabaseConnection.closeConnection();
                        executando = false;
                    }
                    default -> view.exibirMensagemErro("Opcao nao reconhecida. Por favor, escolha um numero do menu.");
                }

                if (executando) {
                    view.aguardarEnter();
                }

            } catch (Exception e) {
                view.exibirMensagemErro("Ocorreu um imprevisto na operacao: " + e.getMessage());
                view.aguardarEnter();
            }
        }
    }

    /**
     * [Opcao 1] Fluxo guiado para o paciente confirmar a ingestao da dose.
     * Mostra os remedios devidos agora e permite confirmar um por um ou todos de uma vez (opcao [T]).
     */
    private static void tomarMedicamento(Paciente paciente, Responsavel responsavel) throws SQLException {
        List<Dose> devidasAgora = doseDAO.listarDosesParaTomarAgora(paciente.getId());

        if (devidasAgora.isEmpty()) {
            List<Dose> proximas = doseDAO.listarPendentesPorPaciente(paciente.getId());
            if (proximas.isEmpty()) {
                view.exibirMensagemSucesso("Que maravilha! Voce nao tem nenhuma dose agendada pendente.");
                return;
            }

            Dose proxima = proximas.get(0);
            view.exibirMensagemSucesso("Voce esta em dia! Todos os remedios deste horario ja foram tomados.");
            System.out.printf("Proximo remedio na rotina: '%s' (%s)\n",
                    proxima.getNomeMedicamento(), proxima.getHorarioPrevistoFormatado());

            boolean adiantar = view.lerConfirmacaoSN("Deseja antecipar e registrar a tomada dessa proxima dose agora mesmo? (S/N): ");
            if (adiantar) {
                Medicamento medAtualizado = doseDAO.confirmarTomadaDose(proxima.getId());
                view.exibirConfirmacaoDoseSucesso(medAtualizado, responsavel);
            } else {
                System.out.println("Operacao cancelada. Medicamento mantido para o horario previsto.");
            }
            return;
        }

        view.exibirListaMedicamentosParaTomar(devidasAgora);
        String entrada = view.lerOpcaoTomadaDose("Digite o numero do remedio, [T] para tomar todos ou [0] para voltar: ");

        if ("0".equals(entrada)) {
            System.out.println("Operacao cancelada. Nenhuma alteracao foi realizada.");
            return;
        }

        if ("T".equals(entrada)) {
            List<Integer> ids = devidasAgora.stream().map(Dose::getId).toList();
            List<Medicamento> atualizados = doseDAO.confirmarTodasDosesAgora(ids);
            view.exibirConfirmacaoLoteSucesso(atualizados, responsavel);
            return;
        }

        try {
            int escolha = Integer.parseInt(entrada);
            if (escolha < 1 || escolha > devidasAgora.size()) {
                view.exibirMensagemErro("Opcao invalida. Escolha um numero entre 1 e " + devidasAgora.size() + ".");
                return;
            }

            Dose doseEscolhida = devidasAgora.get(escolha - 1);
            Medicamento medAtualizado = doseDAO.confirmarTomadaDose(doseEscolhida.getId());
            view.exibirConfirmacaoDoseSucesso(medAtualizado, responsavel);

        } catch (NumberFormatException e) {
            view.exibirMensagemErro("Entrada nao reconhecida. Digite o numero da lista ou [T] para todos.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            view.exibirMensagemErro(e.getMessage());
        }
    }

    /**
     * [Opcao 2] Exibe a agenda organizada de remedios e o historico recente.
     */
    private static void verMinhaRotina(Paciente paciente) throws SQLException {
        List<Dose> devidasAgora = doseDAO.listarDosesParaTomarAgora(paciente.getId());
        List<Dose> proximasHoje = doseDAO.listarProximasHoje(paciente.getId());
        List<Dose> proximosDias = doseDAO.listarProximosDias(paciente.getId());
        List<Dose> historico = doseDAO.listarHistoricoRecente(paciente.getId(), 5);

        view.exibirMinhaRotina(devidasAgora, proximasHoje, proximosDias, historico);
    }

    /**
     * [Opcao 3] Consulta o catalogo de medicamentos, posologia, estoque e autonomia.
     */
    private static void verMeusMedicamentos(Paciente paciente) throws SQLException {
        List<Medicamento> medicamentos = medicamentoDAO.listarPorUsuario(paciente.getId());
        view.exibirCatalogoMedicamentos(medicamentos);
    }

    /**
     * [Opcao 4] Reabastecimento intuitivo de estoque (Chegou caixinha nova).
     * Permite somar comprimidos comprados na farmacia ou retificar contagem fisica (RF07 / RN07).
     */
    private static void reporEstoque(Paciente paciente, Responsavel responsavel) throws SQLException {
        List<Medicamento> medicamentos = medicamentoDAO.listarPorUsuario(paciente.getId());

        if (medicamentos.isEmpty()) {
            view.exibirMensagemAviso("Voce ainda nao possui medicamentos cadastrados para repor estoque.");
            return;
        }

        view.exibirMenuReporEstoque(medicamentos);
        int escolha = view.lerInteiroComFaixa("Escolha o medicamento [1-" + medicamentos.size() + "] ou 0 para voltar: ", 0, medicamentos.size());

        if (escolha == 0) {
            return;
        }

        Medicamento med = medicamentos.get(escolha - 1);
        view.exibirSubmenuModoReposicao(med);
        int modo = view.lerInteiroComFaixa("Escolha a opcao [0-2]: ", 0, 2);

        if (modo == 0) {
            return;
        }

        if (modo == 1) {
            int comprados = view.lerInteiroPositivo("Quantos comprimidos vieram na embalagem nova? (ex: 30): ", 1);
            int estoqueAnterior = med.getQuantidadeEstoque();
            med.adicionarEstoque(comprados);
            medicamentoDAO.atualizarEstoque(med.getId(), med.getQuantidadeEstoque());
            view.exibirResultadoReposicao(med, estoqueAnterior, comprados);

        } else if (modo == 2) {
            int novoEstoque = view.lerInteiroPositivo("Informe o total exato de comprimidos conferidos no armario: ", 0);
            med.setQuantidadeEstoque(novoEstoque);
            medicamentoDAO.atualizarEstoque(med.getId(), novoEstoque);
            view.exibirResultadoConferenciaFisica(med, novoEstoque);
        }
    }

    /**
     * [Opcao 5] Cadastro de novo medicamento com linguagem acolhedora (Create do CRUD).
     */
    private static void cadastrarNovoTratamento(Paciente paciente) throws SQLException {
        view.exibirCabecalhoNovoTratamento();

        String nome = view.lerTextoNaoVazio("* Nome do medicamento e dosagem (ex: Sinvastatina 20mg): ",
                "O nome do medicamento nao pode ficar em branco.");

        int estoqueInicial = view.lerInteiroPositivo("* Quantos comprimidos voce tem em casa agora? (ex: 30): ", 0);

        int frequenciaHoras = view.lerInteiroPositivo(
                "* De quantas em quantas horas deve tomar? (ex: 8 para 3x/dia, 12 para 2x/dia, 24 para 1x/dia): ", 1);

        int qtdPorDose = view.lerInteiroComPadrao(
                "* Quantos comprimidos toma por vez? [Pressione ENTER para 1]: ", 1, 1);

        Medicamento novo = new Medicamento(paciente.getId(), nome, estoqueInicial, frequenciaHoras, qtdPorDose);
        medicamentoDAO.inserir(novo);

        // Gera automaticamente a primeira agenda de doses do tratamento
        doseDAO.gerarAgendaFutura(novo, 4);

        view.exibirConfirmacaoNovoTratamento(novo);
    }

    /**
     * [Opcao 6] Gestao de Medicamentos (Alterar dados posologicos ou encerrar tratamento).
     * Realiza o Update e Delete do CRUD de forma segura, transacional e humanizada.
     */
    private static void gerenciarMedicamentos(Paciente paciente) throws SQLException {
        List<Medicamento> medicamentos = medicamentoDAO.listarPorUsuario(paciente.getId());

        if (medicamentos.isEmpty()) {
            view.exibirMensagemAviso("Voce nao possui medicamentos cadastrados para gerenciar.");
            return;
        }

        view.exibirMenuGerenciarMedicamentos(medicamentos);
        int acao = view.lerInteiroComFaixa("Escolha o que deseja fazer [0-2]: ", 0, 2);

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
            int esc = view.lerInteiroComFaixa("Escolha o medicamento [1-" + medicamentos.size() + "]: ", 1, medicamentos.size());
            Medicamento med = medicamentos.get(esc - 1);

            System.out.println("\nAtualizacao de dados de '" + med.getNome() + "' (ENTER mantem o valor atual):");
            String novoNome = view.lerTexto("Novo nome comercial [" + med.getNome() + "]: ");
            if (!novoNome.isEmpty()) {
                med.setNome(novoNome);
            }

            int novaFreq = view.lerInteiroComPadrao("Nova frequencia em horas [" + med.getFrequenciaHoras() + "h]: ",
                    med.getFrequenciaHoras(), 1);
            med.setFrequenciaHoras(novaFreq);

            int novaQtdDose = view.lerInteiroComPadrao("Nova quantidade por dose [" + med.getQuantidadePorDose() + "]: ",
                    med.getQuantidadePorDose(), 1);
            med.setQuantidadePorDose(novaQtdDose);

            medicamentoDAO.atualizar(med);
            view.exibirMensagemSucesso("Dados do medicamento atualizados com sucesso!");
            System.out.printf("Nova autonomia calculada: Dura %d dias de tratamento.\n", med.calcularDiasRestantes());

        } else if (acao == 2) {
            // Delete do CRUD
            System.out.println("\nSelecione qual medicamento deseja remover do tratamento:");
            for (int i = 0; i < medicamentos.size(); i++) {
                Medicamento m = medicamentos.get(i);
                System.out.printf("  [%d] %s\n", (i + 1), m.getNome());
            }
            int esc = view.lerInteiroComFaixa("Escolha o medicamento [1-" + medicamentos.size() + "]: ", 1, medicamentos.size());
            Medicamento med = medicamentos.get(esc - 1);

            boolean confirmacao = view.lerConfirmacaoSN(
                    "Tem certeza de que o tratamento com '" + med.getNome() + "' foi encerrado e deseja remove-lo? (S/N): ");

            if (confirmacao) {
                medicamentoDAO.excluir(med.getId());
                view.exibirMensagemSucesso("Medicamento e historico de doses removidos com sucesso.");
            } else {
                System.out.println("Operacao cancelada. Medicamento mantido ativo.");
            }
        }
    }

    /**
     * [Opcao 7] Painel do Responsavel/Cuidador (RF06 / RN08).
     */
    private static void espacoDaFamilia(Responsavel responsavel, Paciente paciente) throws SQLException {
        List<Medicamento> medicamentos = medicamentoDAO.listarPorUsuario(paciente.getId());
        List<Dose> todasPendentes = doseDAO.listarPendentesPorPaciente(paciente.getId());
        List<Dose> pendentesHoje = todasPendentes.stream().filter(d -> d.isHoje() || d.isDevidaAgora()).toList();
        List<Dose> todasTomadas = doseDAO.listarHistoricoRecente(paciente.getId(), 20);
        List<Dose> tomadasHoje = todasTomadas.stream().filter(Dose::isHoje).toList();

        view.exibirPainelCuidador(responsavel, paciente, medicamentos, pendentesHoje, tomadasHoje);
    }

    /**
     * [Opcao 8] Demonstracao Tecnica e Diretrizes da AEP (Banca Avaliadora).
     * Comprova formalmente POO, JDBC, CRUD e Aderencia ao Planejamento.
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
        return new Paciente(1, "Joao da Silva", "joao@email.com", "123456", "Hipertensao e Diabetes Tipo 2");
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