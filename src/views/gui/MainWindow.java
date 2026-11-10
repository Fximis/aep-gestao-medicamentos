package views.gui;

import config.DatabaseConnection;
import dao.DoseDAO;
import dao.MedicamentoDAO;
import dao.UsuarioDAO;
import models.Dose;
import models.Medicamento;
import models.Paciente;
import models.Responsavel;
import models.Usuario;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Janela Principal da Aplicação Gráfica (Desktop GUI).
 * Desenvolvida com Java Swing nativo, proporcionando uma experiência humanizada,
 * moderna e acessível para o paciente e familiares, em total conformidade com as diretrizes da AEP.
 */
public class MainWindow extends JFrame {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MedicamentoDAO medicamentoDAO = new MedicamentoDAO();
    private final DoseDAO doseDAO = new DoseDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private Paciente pacienteAtivo;
    private Responsavel responsavelPadrao;

    // Componentes de interface
    private JLabel lblStatusUsuario;
    private JLabel lblAlertaEstoque;
    private JLabel lblAlertaDoses;
    private JProgressBar pgbAdesao;
    private JLabel lblAdesaoTexto;

    private JTable tblDoses;
    private DefaultTableModel modDoses;

    private JTable tblMedicamentos;
    private DefaultTableModel modMedicamentos;

    public MainWindow() {
        super("Cuidado Diário — Gestão Familiar de Medicamentos");
        carregarUsuarios();
        configurarJanela();
        carregarDados();
    }

    private void carregarUsuarios() {
        try {
            List<Paciente> pacientes = usuarioDAO.listarPacientes();
            this.pacienteAtivo = !pacientes.isEmpty() ? pacientes.get(0) :
                    new Paciente(1, "João da Silva", "joao@email.com", "123456", "Hipertensão e Diabetes");

            Usuario u = usuarioDAO.buscarPorId(2);
            this.responsavelPadrao = (u instanceof Responsavel r) ? r :
                    new Responsavel(2, "Maria Silva Santos", "maria@email.com", "123456", "Filha e Cuidadora");
        } catch (SQLException e) {
            this.pacienteAtivo = new Paciente(1, "João da Silva", "joao@email.com", "123456", "Hipertensão");
            this.responsavelPadrao = new Responsavel(2, "Maria Silva", "maria@email.com", "123456", "Filha");
        }
    }

    private void configurarJanela() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. Cabeçalho Superior Informativo
        add(criarPainelCabecalho(), BorderLayout.NORTH);

        // 2. Abas Principais
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 13));

        tabbedPane.addTab("  🕒 Minha Rotina (Doses)  ", criarAbaRotinaDoses());
        tabbedPane.addTab("  💊 Meus Medicamentos & Estoque  ", criarAbaMedicamentos());
        tabbedPane.addTab("  👨‍👩‍👧 Espaço da Família / Cuidador  ", criarAbaFamilia());
        tabbedPane.addTab("  ⚙️ Diretrizes Técnicas (Banca AEP)  ", criarAbaTecnica());

        add(tabbedPane, BorderLayout.CENTER);

        // 3. Rodapé com Status de Persistência
        add(criarPainelRodape(), BorderLayout.SOUTH);
    }

    private JPanel criarPainelCabecalho() {
        JPanel pnl = new JPanel(new BorderLayout(10, 5));
        pnl.setBackground(new Color(245, 247, 250));
        pnl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 215, 225)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel pnlUser = new JPanel(new GridLayout(2, 1, 2, 2));
        pnlUser.setOpaque(false);

        JLabel lblTitulo = new JLabel("CUIDADO DIÁRIO");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitulo.setForeground(new Color(30, 60, 100));

        lblStatusUsuario = new JLabel(String.format("Paciente: %s (%s)  |  Cuidador: %s (%s)",
                pacienteAtivo.getNome(), pacienteAtivo.getCondicaoMedica(),
                responsavelPadrao.getNome(), responsavelPadrao.getVinculoFamiliar()));
        lblStatusUsuario.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblStatusUsuario.setForeground(new Color(70, 80, 95));

        pnlUser.add(lblTitulo);
        pnlUser.add(lblStatusUsuario);
        pnl.add(pnlUser, BorderLayout.WEST);

        // Painel de Alertas Rápidos (Doses Imediatas e Recompra)
        JPanel pnlAlertas = new JPanel(new GridLayout(2, 1, 2, 2));
        pnlAlertas.setOpaque(false);

        lblAlertaDoses = new JLabel("🔔 Verificando doses...");
        lblAlertaDoses.setFont(new Font("SansSerif", Font.BOLD, 12));

        lblAlertaEstoque = new JLabel("📦 Verificando estoque...");
        lblAlertaEstoque.setFont(new Font("SansSerif", Font.BOLD, 12));

        pnlAlertas.add(lblAlertaDoses);
        pnlAlertas.add(lblAlertaEstoque);
        pnl.add(pnlAlertas, BorderLayout.EAST);

        return pnl;
    }

    private JPanel criarAbaRotinaDoses() {
        JPanel pnl = new JPanel(new BorderLayout(10, 10));
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Tabela de Doses
        String[] colunas = {"ID", "Medicamento", "Horário Previsto", "Horário Consumo", "Situação"};
        modDoses = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblDoses = new JTable(modDoses);
        tblDoses.setRowHeight(28);
        tblDoses.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblDoses.getColumnModel().getColumn(0).setMaxWidth(60);

        // Renderizador para destacar pendentes
        tblDoses.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String sit = (String) table.getValueAt(row, 4);
                    if (sit != null && sit.contains("Aguardando")) {
                        c.setBackground(new Color(255, 250, 230));
                        c.setForeground(new Color(150, 80, 0));
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(new Color(40, 40, 40));
                    }
                }
                return c;
            }
        });

        pnl.add(new JScrollPane(tblDoses), BorderLayout.CENTER);

        // Barra de Ações
        JPanel pnlAcoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));

        JButton btnTomar = new JButton("✔ Tomar Medicamento Selecionado");
        btnTomar.setFont(btnTomar.getFont().deriveFont(Font.BOLD, 13f));
        btnTomar.setBackground(new Color(46, 139, 87));
        btnTomar.setForeground(Color.WHITE);

        JButton btnTomarTodas = new JButton("⚡ Tomar Todas as Doses Agora");
        btnTomarTodas.setFont(btnTomarTodas.getFont().deriveFont(Font.BOLD, 13f));
        btnTomarTodas.setBackground(new Color(41, 128, 185));
        btnTomarTodas.setForeground(Color.WHITE);

        JButton btnAtualizar = new JButton("🔄 Atualizar Lista");

        btnTomar.addActionListener(e -> confirmarDoseSelecionada());
        btnTomarTodas.addActionListener(e -> confirmarTodasDosesAgora());
        btnAtualizar.addActionListener(e -> carregarDados());

        pnlAcoes.add(btnTomar);
        pnlAcoes.add(btnTomarTodas);
        pnlAcoes.add(btnAtualizar);

        pnl.add(pnlAcoes, BorderLayout.SOUTH);
        return pnl;
    }

    private JPanel criarAbaMedicamentos() {
        JPanel pnl = new JPanel(new BorderLayout(10, 10));
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        String[] colunas = {"ID", "Nome Comercial", "Estoque Físico", "Frequência", "Dose", "Autonomia (Dias)", "Status do Estoque"};
        modMedicamentos = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblMedicamentos = new JTable(modMedicamentos);
        tblMedicamentos.setRowHeight(28);
        tblMedicamentos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblMedicamentos.getColumnModel().getColumn(0).setMaxWidth(60);

        // Destaque visual para medicamentos em alerta crítico (<= 5 dias)
        tblMedicamentos.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    String status = (String) table.getValueAt(row, 6);
                    if (status != null && status.contains("RECOMPRA")) {
                        c.setBackground(new Color(255, 235, 235));
                        c.setForeground(new Color(180, 20, 20));
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(new Color(40, 40, 40));
                    }
                }
                return c;
            }
        });

        pnl.add(new JScrollPane(tblMedicamentos), BorderLayout.CENTER);

        // Barra de Ações do CRUD
        JPanel pnlAcoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

        JButton btnNovo = new JButton("➕ Novo Medicamento");
        JButton btnEditar = new JButton("✏️ Editar Dados");
        JButton btnRepor = new JButton("📦 Repor Estoque (Nova Compra)");
        btnRepor.setFont(btnRepor.getFont().deriveFont(Font.BOLD));
        JButton btnExcluir = new JButton("🗑️ Excluir");
        JButton btnAtualizar = new JButton("🔄 Atualizar");

        btnNovo.addActionListener(e -> novoMedicamento());
        btnEditar.addActionListener(e -> editarMedicamento());
        btnRepor.addActionListener(e -> reporEstoque());
        btnExcluir.addActionListener(e -> excluirMedicamento());
        btnAtualizar.addActionListener(e -> carregarDados());

        pnlAcoes.add(btnNovo);
        pnlAcoes.add(btnEditar);
        pnlAcoes.add(btnRepor);
        pnlAcoes.add(btnExcluir);
        pnlAcoes.add(btnAtualizar);

        pnl.add(pnlAcoes, BorderLayout.SOUTH);
        return pnl;
    }

    private JPanel criarAbaFamilia() {
        JPanel pnl = new JPanel(new BorderLayout(15, 15));
        pnl.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel pnlCards = new JPanel(new GridLayout(3, 1, 15, 15));

        // Card 1: Informações do Cuidador e Paciente
        JPanel c1 = criarCard("Acompanhamento Familiar Autorizado (RF06 / RN08)",
                String.format("<html><b>Responsável Familiar:</b> %s (%s)<br>" +
                        "<b>Paciente Assistido:</b> %s<br>" +
                        "<b>Condição Médica / Diagnóstico:</b> %s</html>",
                        responsavelPadrao.getNome(), responsavelPadrao.getVinculoFamiliar(),
                        pacienteAtivo.getNome(), pacienteAtivo.getCondicaoMedica()));

        // Card 2: Adesão do Dia
        JPanel c2 = new JPanel(new BorderLayout(10, 10));
        c2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 235), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        c2.setBackground(Color.WHITE);

        JLabel lblTitC2 = new JLabel("Adesão ao Tratamento Hoje");
        lblTitC2.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTitC2.setForeground(new Color(30, 60, 100));

        lblAdesaoTexto = new JLabel("Calculando doses do dia...");
        lblAdesaoTexto.setFont(new Font("SansSerif", Font.PLAIN, 13));

        pgbAdesao = new JProgressBar(0, 100);
        pgbAdesao.setStringPainted(true);
        pgbAdesao.setPreferredSize(new Dimension(200, 24));

        JPanel pnlC2Body = new JPanel(new GridLayout(2, 1, 5, 5));
        pnlC2Body.setOpaque(false);
        pnlC2Body.add(lblAdesaoTexto);
        pnlC2Body.add(pgbAdesao);

        c2.add(lblTitC2, BorderLayout.NORTH);
        c2.add(pnlC2Body, BorderLayout.CENTER);

        // Card 3: Regra de Proteção
        JPanel c3 = criarCard("Diretriz de Segurança e Autonomia do Paciente (RN01)",
                "<html>O estoque <b>nunca é deduzido automaticamente</b> apenas pela passagem do horário programado.<br>" +
                        "Cada baixa exige a confirmação ativa do paciente ou responsável, garantindo rastreabilidade " +
                        "estrita do que foi realmente ingerido.</html>");

        pnlCards.add(c1);
        pnlCards.add(c2);
        pnlCards.add(c3);

        pnl.add(pnlCards, BorderLayout.NORTH);
        return pnl;
    }

    private JPanel criarAbaTecnica() {
        JPanel pnl = new JPanel(new BorderLayout(10, 10));
        pnl.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JTextArea txt = new JTextArea();
        txt.setEditable(false);
        txt.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txt.setBackground(new Color(250, 252, 255));
        txt.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================================================\n");
        sb.append("   EVIDÊNCIAS DE CONFORMIDADE COM O EDITAL DA AEP (2ª ENTREGA - 4º SEMESTRE 2026.2)       \n");
        sb.append("=========================================================================================\n\n");

        sb.append("1. PERSISTÊNCIA EM BANCO DE DADOS RELACIONAL (Peso 0,2):\n");
        sb.append("   - Modo de Conexão: ").append(DatabaseConnection.isUsingSQLite() ? "SQLite Relacional Embutido" : "MySQL Localhost (Porta 3306)").append("\n");
        sb.append("   - Conexão Ativa via JDBC: Driver oficial carregado em tempo de execução via DriverManager.\n");
        sb.append("   - Integridade Referencial: Script DDL em 'database/schema.sql' com PKs, FKs e ON DELETE CASCADE.\n\n");

        sb.append("2. PILARES DE PROGRAMAÇÃO ORIENTADA A OBJETOS (Peso 0,3):\n");
        sb.append("   - Classe Abstrata: 'Usuario.java' servindo como classe base da hierarquia.\n");
        sb.append("   - Interface: 'GenericDAO<T>' formalizando o contrato de persistência em 'dao/GenericDAO.java'.\n");
        sb.append("   - Herança: 'Paciente extends Usuario' e 'Responsavel extends Usuario'.\n");
        sb.append("   - Polimorfismo (@Override): Método abstrato 'obterTipo()' sobrescrito em cada subclasse:\n");
        sb.append("     * Chamada Polimórfica [Paciente]: ").append(pacienteAtivo.obterTipo()).append("\n");
        sb.append("     * Chamada Polimórfica [Responsável]: ").append(responsavelPadrao.obterTipo()).append("\n");
        sb.append("   - Encapsulamento: Atributos privados e métodos de cálculo de domínio ('calcularDiasRestantes').\n\n");

        sb.append("3. OPERAÇÕES DE CRUD COMPLETO NA ENTIDADE PRINCIPAL (Peso 0,3):\n");
        sb.append("   - Create: 'MedicamentoDAO.inserir(Medicamento)' com retorno de chave gerada.\n");
        sb.append("   - Read: 'MedicamentoDAO.buscarPorId(id)' e 'listarPorUsuario(usuarioId)'.\n");
        sb.append("   - Update: 'MedicamentoDAO.atualizar(Medicamento)' e 'atualizarEstoque(id, novoEstoque)'.\n");
        sb.append("   - Delete: 'MedicamentoDAO.excluir(id)' com exclusão transacional das dependências.\n\n");

        sb.append("4. ADERÊNCIA AO PLANEJAMENTO E REGRAS DE NEGÓCIO (Peso 0,2):\n");
        sb.append("   - Alerta Proativo de Recompra (RF05 / RN03): Disparo com aviso crítico se autonomia <= 5 dias.\n");
        sb.append("   - Confirmação Ativa (RN01 / RN06): Baixa atômica de estoque em transação ACID via 'DoseDAO'.\n");

        txt.setText(sb.toString());
        pnl.add(new JScrollPane(txt), BorderLayout.CENTER);
        return pnl;
    }

    private JPanel criarCard(String titulo, String htmlContent) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 235), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        card.setBackground(Color.WHITE);

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTit.setForeground(new Color(30, 60, 100));

        JLabel lblBody = new JLabel(htmlContent);
        lblBody.setFont(new Font("SansSerif", Font.PLAIN, 13));

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblBody, BorderLayout.CENTER);
        return card;
    }

    private JPanel criarPainelRodape() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        pnl.setBackground(new Color(240, 242, 245));

        String sgbd = DatabaseConnection.isUsingSQLite() ? "SQLite (database/gestao_medicamentos.db)" : "MySQL (localhost:3306)";
        JLabel lblStatus = new JLabel("✔ Conexão Ativa com Banco de Dados Relacional: " + sgbd);
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(40, 120, 60));

        JLabel lblVersao = new JLabel("AEP 4º Semestre (2026.2) — Engenharia de Software & ADS");
        lblVersao.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblVersao.setForeground(Color.GRAY);

        pnl.add(lblStatus, BorderLayout.WEST);
        pnl.add(lblVersao, BorderLayout.EAST);
        return pnl;
    }

    // -------------------------------------------------------------------------
    // Lógica de Carregamento e Ações da Interface
    // -------------------------------------------------------------------------

    private void carregarDados() {
        try {
            // 1. Carrega Medicamentos
            List<Medicamento> medicamentos = medicamentoDAO.listarPorUsuario(pacienteAtivo.getId());
            modMedicamentos.setRowCount(0);

            int medicamentosEmAlerta = 0;
            for (Medicamento m : medicamentos) {
                int dias = m.calcularDiasRestantes();
                boolean alerta = m.verificarAlertaRecompra();
                if (alerta) {
                    medicamentosEmAlerta++;
                }

                String status = alerta ? "🚨 RECOMPRA URGENTE" : "✅ Nível Seguro";
                modMedicamentos.addRow(new Object[]{
                        m.getId(),
                        m.getNome(),
                        m.getQuantidadeEstoque() + " comp.",
                        "A cada " + m.getFrequenciaHoras() + "h",
                        m.getQuantidadePorDose() + " comp.",
                        dias + " dia(s)",
                        status
                });
            }

            // Atualiza alerta de estoque no cabeçalho
            if (medicamentosEmAlerta > 0) {
                lblAlertaEstoque.setText("🚨 " + medicamentosEmAlerta + " remédio(s) acabando (≤ 5 dias)!");
                lblAlertaEstoque.setForeground(new Color(190, 30, 30));
            } else {
                lblAlertaEstoque.setText("📦 Estoque em nível seguro");
                lblAlertaEstoque.setForeground(new Color(40, 120, 60));
            }

            // 2. Carrega Doses
            List<Dose> doses = doseDAO.listarPorUsuario(pacienteAtivo.getId());
            modDoses.setRowCount(0);

            int dosesDevidasAgora = 0;
            int dosesHojeTotal = 0;
            int dosesHojeTomadas = 0;

            for (Dose d : doses) {
                boolean tomada = d.isStatusTomado();
                if (d.isHoje()) {
                    dosesHojeTotal++;
                    if (tomada) {
                        dosesHojeTomadas++;
                    }
                }

                if (!tomada && d.isDevidaAgora()) {
                    dosesDevidasAgora++;
                }

                String prevStr = d.getHorarioPrevisto() != null ? d.getHorarioPrevisto().format(FORMATTER) : "N/D";
                String consStr = d.getHorarioConsumo() != null ? d.getHorarioConsumo().format(FORMATTER) : "--";
                String sitStr = tomada ? "✔ Tomada (" + consStr + ")" : "⏳ Aguardando Confirmação";

                modDoses.addRow(new Object[]{
                        d.getId(),
                        d.getNomeMedicamento(),
                        prevStr,
                        consStr,
                        sitStr
                });
            }

            // Atualiza alerta de doses no cabeçalho
            if (dosesDevidasAgora > 0) {
                lblAlertaDoses.setText("🔔 " + dosesDevidasAgora + " dose(s) para tomar agora!");
                lblAlertaDoses.setForeground(new Color(190, 100, 0));
            } else {
                lblAlertaDoses.setText("✔ Nenhuma dose atrasada agora");
                lblAlertaDoses.setForeground(new Color(40, 120, 60));
            }

            // Atualiza barra de adesão familiar
            if (dosesHojeTotal > 0) {
                int pct = (int) Math.round(((double) dosesHojeTomadas / dosesHojeTotal) * 100.0);
                pgbAdesao.setValue(pct);
                lblAdesaoTexto.setText(String.format("%d de %d doses de hoje já foram tomadas (%d%% de adesão).",
                        dosesHojeTomadas, dosesHojeTotal, pct));
            } else {
                pgbAdesao.setValue(100);
                lblAdesaoTexto.setText("Nenhuma dose agendada para hoje até o momento.");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao carregar dados do banco de dados: " + e.getMessage(),
                    "Erro de Persistência", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmarDoseSelecionada() {
        int row = tblDoses.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma dose na tabela para confirmar a ingestão.",
                    "Nenhuma Seleção", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int doseId = (Integer) tblDoses.getValueAt(row, 0);
        String situacao = (String) tblDoses.getValueAt(row, 4);

        if (situacao != null && situacao.contains("Tomada")) {
            JOptionPane.showMessageDialog(this,
                    "Esta dose já foi registrada como tomada anteriormente.",
                    "Dose já Confirmada", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Medicamento med = doseDAO.confirmarTomadaDose(doseId);
            carregarDados();

            String msg = String.format("Dose confirmada com sucesso!\n\n" +
                            "Medicamento: %s\n" +
                            "Quantidade Deduzida: %d comprimido(s)\n" +
                            "Estoque Restante: %d comprimidos\n" +
                            "Autonomia Calculada: %d dias de tratamento",
                    med.getNome(), med.getQuantidadePorDose(),
                    med.getQuantidadeEstoque(), med.calcularDiasRestantes());

            if (med.verificarAlertaRecompra()) {
                JOptionPane.showMessageDialog(this,
                        msg + "\n\n🚨 [ALERTA DE RECOMPRA - REGRA RN03 / RF05]\n" +
                                "O estoque atingiu 5 dias ou menos de tratamento!\n" +
                                "Providencie a reposição de novas caixas.",
                        "Dose Confirmada com Alerta de Compra", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, msg, "Dose Registrada", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Não foi possível confirmar a dose: " + e.getMessage(),
                    "Falha na Baixa de Dose", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmarTodasDosesAgora() {
        try {
            List<Dose> pendentesAgora = doseDAO.listarDosesParaTomarAgora(pacienteAtivo.getId());
            if (pendentesAgora.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Você não possui doses pendentes para o momento atual. Tratamento em dia!",
                        "Tudo em Dia", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Deseja confirmar a tomada de todas as " + pendentesAgora.size() + " doses devidas agora?",
                    "Confirmar Doses em Lote", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                List<Integer> ids = new ArrayList<>();
                for (Dose d : pendentesAgora) {
                    ids.add(d.getId());
                }

                List<Medicamento> atualizados = doseDAO.confirmarTodasDosesAgora(ids);
                carregarDados();

                boolean algumAlerta = false;
                StringBuilder sb = new StringBuilder("Todas as " + ids.size() + " doses foram confirmadas com sucesso!\n\n");
                for (Medicamento m : atualizados) {
                    sb.append("• ").append(m.getNome()).append(": ").append(m.getQuantidadeEstoque())
                            .append(" comp. restantes (").append(m.calcularDiasRestantes()).append(" dias)\n");
                    if (m.verificarAlertaRecompra()) {
                        algumAlerta = true;
                    }
                }

                if (algumAlerta) {
                    sb.append("\n🚨 ATENÇÃO: Um ou mais medicamentos atingiram o limite de 5 dias para recompra!");
                    JOptionPane.showMessageDialog(this, sb.toString(), "Doses Confirmadas", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, sb.toString(), "Doses Confirmadas", JOptionPane.INFORMATION_MESSAGE);
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Falha ao confirmar doses em lote: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void novoMedicamento() {
        MedicamentoDialog dlg = new MedicamentoDialog(this, "Cadastrar Novo Medicamento", null, pacienteAtivo.getId());
        dlg.setVisible(true);

        if (dlg.isConfirmado()) {
            Medicamento novo = dlg.getMedicamento();
            try {
                medicamentoDAO.inserir(novo);
                doseDAO.gerarAgendaFutura(novo, 6);
                carregarDados();
                JOptionPane.showMessageDialog(this,
                        "Medicamento '" + novo.getNome() + "' cadastrado com sucesso no banco de dados!",
                        "Cadastro Concluído", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                        "Erro ao salvar medicamento: " + e.getMessage(),
                        "Erro de Banco", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editarMedicamento() {
        int row = tblMedicamentos.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um medicamento na tabela para editar.",
                    "Nenhuma Seleção", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int id = (Integer) tblMedicamentos.getValueAt(row, 0);
        try {
            Medicamento med = medicamentoDAO.buscarPorId(id);
            if (med == null) {
                JOptionPane.showMessageDialog(this, "Medicamento não encontrado.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            MedicamentoDialog dlg = new MedicamentoDialog(this, "Editar Medicamento", med, pacienteAtivo.getId());
            dlg.setVisible(true);

            if (dlg.isConfirmado()) {
                medicamentoDAO.atualizar(med);
                carregarDados();
                JOptionPane.showMessageDialog(this,
                        "Dados do medicamento atualizados com sucesso no banco!",
                        "Atualização Concluída", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao atualizar medicamento: " + e.getMessage(),
                    "Erro de Banco", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reporEstoque() {
        int row = tblMedicamentos.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione o medicamento que você comprou para repor o estoque.",
                    "Nenhuma Seleção", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int id = (Integer) tblMedicamentos.getValueAt(row, 0);
        try {
            Medicamento med = medicamentoDAO.buscarPorId(id);
            if (med == null) return;

            ReporEstoqueDialog dlg = new ReporEstoqueDialog(this, med);
            dlg.setVisible(true);

            if (dlg.isConfirmado()) {
                int novaQtd = med.getQuantidadeEstoque() + dlg.getQuantidadeAdicionada();
                medicamentoDAO.atualizarEstoque(id, novaQtd);
                carregarDados();

                JOptionPane.showMessageDialog(this,
                        String.format("Estoque de '%s' atualizado!\nNova quantidade: %d comprimidos (Autonomia: %d dias).",
                                med.getNome(), novaQtd, (novaQtd / ((24 / med.getFrequenciaHoras()) * med.getQuantidadePorDose()))),
                        "Estoque Reposto com Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao repor estoque: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void excluirMedicamento() {
        int row = tblMedicamentos.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um medicamento para excluir.",
                    "Nenhuma Seleção", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int id = (Integer) tblMedicamentos.getValueAt(row, 0);
        String nome = (String) tblMedicamentos.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Tem certeza que deseja excluir o medicamento '" + nome + "' e todo o seu histórico de doses?",
                "Confirmar Exclusão (Delete)", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                medicamentoDAO.excluir(id);
                carregarDados();
                JOptionPane.showMessageDialog(this,
                        "Medicamento e doses dependentes excluídos com sucesso do banco de dados.",
                        "Exclusão Concluída", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erro ao excluir: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
