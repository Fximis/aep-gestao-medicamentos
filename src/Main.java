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
 * Orquestra as regras de negócio, persistência via DAO e visualização no console.
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

        // 2. Carrega Usuário Padrão para a Sessão (Paciente João da Silva)
        Paciente pacienteAtivo = carregarPacientePadrao();
        Responsavel responsavelPadrao = carregarResponsavelPadrao();

        boolean executando = true;

        while (executando) {
            try {
                view.exibirCabecalho(pacienteAtivo);
                int opcao = view.exibirMenuPrincipal();

                switch (opcao) {
                    case 1 -> cadastrarMedicamento(pacienteAtivo);
                    case 2 -> listarMedicamentos(pacienteAtivo);
                    case 3 -> atualizarMedicamento(pacienteAtivo);
                    case 4 -> ajustarEstoqueManual();
                    case 5 -> excluirMedicamento();
                    case 6 -> exibirAgendaDoses(pacienteAtivo);
                    case 7 -> confirmarTomadaDose();
                    case 8 -> exibirPainelCuidador(responsavelPadrao, pacienteAtivo);
                    case 9 -> demonstrarPolimorfismoPOO();
                    case 0 -> {
                        System.out.println("\nEncerrando o sistema. Banco de dados finalizado com sucesso.");
                        DatabaseConnection.closeConnection();
                        executando = false;
                    }
                    default -> view.exibirMensagemErro("Opção inválida. Tente novamente.");
                }

                if (executando) {
                    view.aguardarEnter();
                }

            } catch (Exception e) {
                view.exibirMensagemErro("Ocorreu uma falha na operação: " + e.getMessage());
                view.aguardarEnter();
            }
        }
    }

    private static void cadastrarMedicamento(Paciente paciente) throws SQLException {
        System.out.println("\n--- [CREATE] CADASTRAR NOVO MEDICAMENTO ---");
        String nome = view.lerTexto("Nome Comercial e Dosagem (ex: Dipirona 500mg): ");
        int estoqueInicial = view.lerInteiro("Quantidade Inicial em Estoque (comprimidos): ");
        int frequenciaHoras = view.lerInteiro("Frequência de Doses (em horas, ex: 8): ");
        int qtdPorDose = view.lerInteiro("Quantidade consumida por dose (ex: 1): ");

        Medicamento novo = new Medicamento(paciente.getId(), nome, estoqueInicial, frequenciaHoras, qtdPorDose);
        medicamentoDAO.inserir(novo);

        // Gera automaticamente as primeiras 4 doses na agenda
        doseDAO.gerarAgendaFutura(novo, 4);

        view.exibirMensagemSucesso("Medicamento cadastrado com sucesso! ID gerado no banco: #" + novo.getId());
        System.out.printf("Autonomia calculada: %d dias de tratamento.\n", novo.calcularDiasRestantes());
        if (novo.verificarAlertaRecompra()) {
            view.exibirAlertaCritico(novo);
        }
    }

    private static void listarMedicamentos(Paciente paciente) throws SQLException {
        List<Medicamento> lista = medicamentoDAO.listarPorUsuario(paciente.getId());
        view.exibirListaMedicamentos(lista);
    }

    private static void atualizarMedicamento(Paciente paciente) throws SQLException {
        System.out.println("\n--- [UPDATE] ATUALIZAR DADOS DO MEDICAMENTO ---");
        listarMedicamentos(paciente);
        int id = view.lerInteiro("\nInforme o ID do medicamento que deseja atualizar: ");

        Medicamento med = medicamentoDAO.buscarPorId(id);
        if (med == null) {
            view.exibirMensagemErro("Medicamento com ID #" + id + " não encontrado.");
            return;
        }

        String novoNome = view.lerTexto("Novo Nome Comercial [" + med.getNome() + "]: ");
        if (!novoNome.isEmpty()) {
            med.setNome(novoNome);
        }

        int novaFreq = view.lerInteiro("Nova Frequência em horas [" + med.getFrequenciaHoras() + "h]: ");
        med.setFrequenciaHoras(novaFreq);

        int novaQtdDose = view.lerInteiro("Nova Quantidade por dose [" + med.getQuantidadePorDose() + "]: ");
        med.setQuantidadePorDose(novaQtdDose);

        medicamentoDAO.atualizar(med);
        view.exibirMensagemSucesso("Dados do medicamento atualizados com sucesso no banco de dados.");
    }

    private static void ajustarEstoqueManual() throws SQLException {
        System.out.println("\n--- [UPDATE ESTOQUE] AJUSTE MANUAL POR COMPRA OU CONFERÊNCIA (RF07) ---");
        int id = view.lerInteiro("Informe o ID do medicamento: ");

        Medicamento med = medicamentoDAO.buscarPorId(id);
        if (med == null) {
            view.exibirMensagemErro("Medicamento não localizado.");
            return;
        }

        System.out.printf("Medicamento: %s | Estoque Atual: %d comprimidos.\n", med.getNome(), med.getQuantidadeEstoque());
        int novoEstoque = view.lerInteiro("Informe a nova quantidade total de comprimidos: ");

        medicamentoDAO.atualizarEstoque(id, novoEstoque);
        med.setQuantidadeEstoque(novoEstoque);

        view.exibirMensagemSucesso("Estoque atualizado com sucesso.");
        System.out.printf("Nova autonomia: %d dias de tratamento.\n", med.calcularDiasRestantes());
        if (med.verificarAlertaRecompra()) {
            view.exibirAlertaCritico(med);
        }
    }

    private static void excluirMedicamento() throws SQLException {
        System.out.println("\n--- [DELETE] EXCLUIR MEDICAMENTO DO BANCO DE DADOS ---");
        int id = view.lerInteiro("Informe o ID do medicamento a ser removido: ");

        Medicamento med = medicamentoDAO.buscarPorId(id);
        if (med == null) {
            view.exibirMensagemErro("Medicamento não encontrado.");
            return;
        }

        String confirmacao = view.lerTexto("Tem certeza que deseja excluir '" + med.getNome() + "' e todo o histórico? (S/N): ");
        if ("S".equalsIgnoreCase(confirmacao)) {
            medicamentoDAO.excluir(id);
            view.exibirMensagemSucesso("Medicamento e doses associadas excluídos com sucesso do banco.");
        } else {
            System.out.println("Operação cancelada pelo usuário.");
        }
    }

    private static void exibirAgendaDoses(Paciente paciente) throws SQLException {
        List<Dose> dosesPendentes = doseDAO.listarPendentesPorPaciente(paciente.getId());
        view.exibirAgendaDoses(dosesPendentes);
    }

    private static void confirmarTomadaDose() throws SQLException {
        System.out.println("\n--- [CONFIRMAÇÃO DE DOSE] BAIXA DE ESTOQUE E ALERTA (RN01, RN03, RN06) ---");
        int doseId = view.lerInteiro("Informe o ID da Dose a ser confirmada: ");

        try {
            Medicamento medAtualizado = doseDAO.confirmarTomadaDose(doseId);
            view.exibirMensagemSucesso("Dose #" + doseId + " confirmada com sucesso!");
            System.out.printf("Quantidade deduzida: %d comprimido(s).\n", medAtualizado.getQuantidadePorDose());
            System.out.printf("Novo estoque de '%s': %d comprimidos.\n", medAtualizado.getNome(), medAtualizado.getQuantidadeEstoque());
            System.out.printf("Autonomia restante calculada: %d dias.\n", medAtualizado.calcularDiasRestantes());

            // Verificação do Alerta de 5 Dias
            if (medAtualizado.verificarAlertaRecompra()) {
                view.exibirAlertaCritico(medAtualizado);
            } else {
                System.out.println("Status do Estoque: [Nível Seguro]");
            }

        } catch (IllegalStateException | IllegalArgumentException e) {
            view.exibirMensagemErro(e.getMessage());
        }
    }

    private static void exibirPainelCuidador(Responsavel responsavel, Paciente paciente) throws SQLException {
        List<Medicamento> medicamentos = medicamentoDAO.listarPorUsuario(paciente.getId());
        view.exibirPainelCuidador(responsavel, paciente, medicamentos);
    }

    private static void demonstrarPolimorfismoPOO() throws SQLException {
        System.out.println("\n--- COMPROVAÇÃO DOS PILARES DE POO (HERANÇA E POLIMORFISMO COM @Override) ---");
        List<Usuario> usuarios = usuarioDAO.listarTodos();

        System.out.println("Iterando sobre a coleção polimórfica List<Usuario>:");
        for (Usuario u : usuarios) {
            // Chamada polimórfica: cada subclasse executa sua própria implementação sobrescrita de obterTipo()
            System.out.printf("  -> Classe: %-15s | Polimorfismo obterTipo(): %-12s | Nome: %s\n",
                    u.getClass().getSimpleName(), u.obterTipo(), u.getNome());
        }
        System.out.println("\nPilares comprovados: Classe Abstrata (Usuario), Herança (extends), Polimorfismo (@Override) e Encapsulamento.");
    }

    private static Paciente carregarPacientePadrao() {
        try {
            List<Paciente> pacientes = usuarioDAO.listarPacientes();
            if (!pacientes.isEmpty()) {
                return pacientes.get(0);
            }
        } catch (SQLException ignored) {
        }
        return new Paciente(1, "João da Silva", "joao@email.com", "123456", "Hipertensão e Diabetes");
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
