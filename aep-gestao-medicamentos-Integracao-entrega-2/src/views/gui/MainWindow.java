package views.gui;

import auth.Autenticacao;
import dao.DoseDAO;
import dao.FamiliarDAO;
import dao.MedicamentoDAO;
import models.Dose;
import models.Medicamento;
import models.Paciente;
import models.Usuario;

import javax.swing.*;
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
    private final FamiliarDAO familiarDAO = new FamiliarDAO();
    private final Paciente pacienteAtivo;
    private final Autenticacao.Sessao sessao;
    private final Autenticacao autenticacao;

    // Componentes de interface
    private JLabel lblStatusUsuario;
    private JLabel lblAlertaEstoque;
    private JLabel lblAlertaDoses;
    private JProgressBar pgbAdesao;
    private JLabel lblAdesaoTexto;
    private JLabel lblResumoAdesao;
    private JPanel listaResponsaveis;

    private JTable tblDoses;
    private DefaultTableModel modDoses;

    private JTable tblMedicamentos;
    private DefaultTableModel modMedicamentos;

    public MainWindow(Autenticacao.Sessao sessao, Autenticacao autenticacao) {
        super("Cuidado Diário | Gestão Familiar de Medicamentos");
        if (!(sessao.usuario() instanceof Paciente paciente)) throw new IllegalArgumentException("Perfil inválido.");
        this.pacienteAtivo = paciente;
        this.sessao = sessao;
        this.autenticacao = autenticacao;
        configurarJanela();
        carregarDados();
    }

    private void configurarJanela() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension tela = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1280, tela.width - 40), Math.min(850, tela.height - 60));
        setMinimumSize(new Dimension(1020, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Tema.FUNDO);

        CardLayout navegacao = new CardLayout();
        JPanel paginas = new JPanel(navegacao);
        paginas.setOpaque(false);
        paginas.add(criarAbaRotinaDoses(), "rotina");
        paginas.add(criarAbaMedicamentos(), "medicamentos");
        paginas.add(criarAbaFamilia(), "familia");

        JPanel conteudo = new JPanel(new BorderLayout(0, 20));
        conteudo.setBorder(BorderFactory.createEmptyBorder(28, 28, 14, 28));
        conteudo.add(criarPainelCabecalho(), BorderLayout.NORTH);
        conteudo.add(paginas, BorderLayout.CENTER);
        conteudo.add(criarPainelRodape(), BorderLayout.SOUTH);
        add(criarMenu(navegacao, paginas), BorderLayout.WEST);
        add(conteudo, BorderLayout.CENTER);
    }

    private JPanel criarMenu(CardLayout navegacao, JPanel paginas) {
        JPanel menu = new JPanel(new BorderLayout());
        menu.setPreferredSize(new Dimension(206, 0));
        menu.setBackground(Tema.MENU);
        menu.setBorder(BorderFactory.createEmptyBorder(32, 18, 24, 18));

        JPanel marca = new JPanel(new GridLayout(3, 1, 0, 4));
        marca.setOpaque(false);
        marca.add(Tema.texto("+  cuidado", 25, true, Color.WHITE));
        marca.add(Tema.texto("     diário", 25, true, Color.WHITE));
        marca.add(Tema.texto("   Mais cuidado. Mais você.", 10, false, new Color(163, 200, 186)));
        menu.add(marca, BorderLayout.NORTH);

        JPanel links = new JPanel();
        links.setOpaque(false);
        links.setLayout(new BoxLayout(links, BoxLayout.Y_AXIS));
        links.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        JLabel secao = Tema.texto("SEU ESPAÇO", 10, true, new Color(151, 189, 176));
        secao.setAlignmentX(Component.LEFT_ALIGNMENT);
        links.add(secao);
        links.add(Box.createVerticalStrut(18));
        ButtonGroup grupo = new ButtonGroup();
        String[] titulos = {"01   Minha rotina", "02   Medicamentos", "03   Minha família"};
        String[] ids = {"rotina", "medicamentos", "familia"};
        for (int i = 0; i < titulos.length; i++) {
            final String pagina = ids[i];
            JToggleButton botao = new JToggleButton(titulos[i]);
            Tema.estilizarBotao(botao, true);
            botao.setHorizontalAlignment(SwingConstants.LEFT);
            botao.setMaximumSize(new Dimension(180, 46));
            botao.setAlignmentX(Component.LEFT_ALIGNMENT);
            botao.setSelected(i == 0);
            botao.setBackground(i == 0 ? Tema.VERDE : Tema.MENU);
            botao.addItemListener(e -> {
                botao.setBackground(botao.isSelected() ? Tema.VERDE : Tema.MENU);
                botao.setForeground(botao.isSelected() ? Color.WHITE : new Color(185, 211, 200));
            });
            if (i != 0) botao.setForeground(new Color(185, 211, 200));
            botao.addActionListener(e -> {
                if ("familia".equals(pagina)) carregarResponsaveis();
                navegacao.show(paginas, pagina);
            });
            grupo.add(botao);
            links.add(botao);
            links.add(Box.createVerticalStrut(8));
        }
        menu.add(links, BorderLayout.CENTER);
        JPanel conta = new JPanel(new GridLayout(0, 1, 0, 8));
        conta.setOpaque(false);
        JButton senha = Tema.botao("Alterar senha", false);
        senha.addActionListener(e -> ContaDialog.alterarSenha(this, autenticacao, sessao));
        JButton sair = Tema.botao("Sair da conta", false);
        sair.addActionListener(e -> ContaDialog.sair(this, autenticacao, sessao));
        conta.add(senha);
        conta.add(sair);
        menu.add(conta, BorderLayout.SOUTH);
        return menu;
    }

    private JPanel criarPainelCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout(0, 24));
        cabecalho.setOpaque(false);
        JPanel topo = new JPanel(new BorderLayout(12, 8));
        topo.setOpaque(false);
        JPanel saudacao = new JPanel();
        saudacao.setLayout(new BoxLayout(saudacao, BoxLayout.Y_AXIS));
        saudacao.setOpaque(false);
        saudacao.add(Tema.texto("BEM-VINDO AO SEU ESPAÇO DE CUIDADO", 10, true, Tema.VERDE));
        saudacao.add(Box.createVerticalStrut(10));
        saudacao.add(Tema.texto("Seu cuidado, em dia.", 29, true, Tema.TEXTO));
        saudacao.add(Box.createVerticalStrut(10));
        lblStatusUsuario = Tema.texto("Paciente: " + pacienteAtivo.getNome(), 12, false, Tema.SUAVE);
        saudacao.add(lblStatusUsuario);
        topo.add(saudacao, BorderLayout.CENTER);
        JPanel data = new JPanel();
        data.setLayout(new BoxLayout(data, BoxLayout.Y_AXIS));
        data.setOpaque(false);
        data.add(Tema.texto("HOJE", 10, true, Tema.SUAVE));
        data.add(Box.createVerticalStrut(10));
        data.add(Tema.texto(java.time.LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd 'de' MMMM", java.util.Locale.forLanguageTag("pt-BR"))),
                14, true, Tema.TEXTO));
        topo.add(data, BorderLayout.EAST);
        cabecalho.add(topo, BorderLayout.NORTH);

        JPanel resumos = new JPanel(new GridLayout(1, 3, 14, 0));
        resumos.setOpaque(false);
        lblAlertaDoses = Tema.texto("Consultando...", 15, true, Tema.TEXTO);
        lblAlertaEstoque = Tema.texto("Consultando...", 15, true, Tema.TEXTO);
        lblResumoAdesao = Tema.texto("Consultando...", 15, true, Tema.VERDE);
        resumos.add(criarResumo("DOSES PARA AGORA", lblAlertaDoses, "Confirme após tomar o medicamento"));
        resumos.add(criarResumo("ATENÇÃO AO ESTOQUE", lblAlertaEstoque, "Alerta preventivo com até 5 dias"));
        resumos.add(criarResumo("SEU PROGRESSO HOJE", lblResumoAdesao, "Cada dose conta para o seu cuidado"));
        cabecalho.add(resumos, BorderLayout.CENTER);
        return cabecalho;
    }

    private JPanel criarResumo(String titulo, JLabel valor, String detalhe) {
        JPanel card = Tema.card(new GridLayout(3, 1, 0, 9));
        card.add(Tema.texto(titulo, 10, true, Tema.SUAVE));
        card.add(valor);
        card.add(Tema.texto(detalhe, 10, false, Tema.SUAVE));
        return card;
    }

    private JPanel criarPagina(String titulo, String descricao, JComponent corpo, JPanel acoes) {
        JPanel pagina = new JPanel(new BorderLayout(0, 18));
        pagina.setOpaque(false);
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 6));
        textos.setOpaque(false);
        textos.add(Tema.texto(titulo, 21, true, Tema.TEXTO));
        textos.add(Tema.texto(descricao, 12, false, Tema.SUAVE));
        pagina.add(textos, BorderLayout.NORTH);
        pagina.add(corpo, BorderLayout.CENTER);
        if (acoes != null) pagina.add(acoes, BorderLayout.SOUTH);
        return pagina;
    }

    private JPanel criarAbaRotinaDoses() {
        String[] colunas = {"ID", "MEDICAMENTO", "PREVISTO PARA", "CONSUMO", "SITUAÇÃO"};
        modDoses = modeloTabela(colunas);
        tblDoses = new JTable(modDoses);
        Tema.tabela(tblDoses, 4, "Aguardando");
        tblDoses.getColumnModel().getColumn(0).setMinWidth(52);
        tblDoses.getColumnModel().getColumn(0).setMaxWidth(52);
        tblDoses.getColumnModel().getColumn(1).setPreferredWidth(245);
        tblDoses.getColumnModel().getColumn(2).setPreferredWidth(150);
        tblDoses.getColumnModel().getColumn(3).setPreferredWidth(145);
        tblDoses.getColumnModel().getColumn(4).setPreferredWidth(185);
        JPanel tabela = Tema.card(new BorderLayout());
        tabela.add(Tema.rolagem(tblDoses));
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acoes.setOpaque(false);
        JButton tomar = Tema.botao("Confirmar dose selecionada", true);
        JButton todas = Tema.botao("Confirmar doses de agora", false);
        JButton atualizar = Tema.botao("Atualizar", false);
        tomar.addActionListener(e -> confirmarDoseSelecionada());
        todas.addActionListener(e -> confirmarTodasDosesAgora());
        atualizar.addActionListener(e -> carregarDados());
        acoes.add(tomar);
        acoes.add(todas);
        acoes.add(atualizar);
        return criarPagina("Minha rotina", "Seus horários e confirmações, em um só lugar. Selecione uma dose para continuar.", tabela, acoes);
    }

    private DefaultTableModel modeloTabela(String[] colunas) {
        return new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private JPanel criarAbaMedicamentos() {
        String[] colunas = {"ID", "MEDICAMENTO", "ESTOQUE", "INTERVALO", "DOSE", "AUTONOMIA", "SITUAÇÃO"};
        modMedicamentos = modeloTabela(colunas);
        tblMedicamentos = new JTable(modMedicamentos);
        Tema.tabela(tblMedicamentos, 6, "Recomprar");
        tblMedicamentos.getColumnModel().getColumn(0).setMinWidth(52);
        tblMedicamentos.getColumnModel().getColumn(0).setMaxWidth(52);
        tblMedicamentos.getColumnModel().getColumn(1).setPreferredWidth(240);
        tblMedicamentos.getColumnModel().getColumn(6).setPreferredWidth(125);
        JPanel tabela = Tema.card(new BorderLayout());
        tabela.add(Tema.rolagem(tblMedicamentos));
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acoes.setOpaque(false);
        JButton novo = Tema.botao("+ Novo medicamento", true);
        JButton repor = Tema.botao("Repor estoque", false);
        JButton editar = Tema.botao("Editar", false);
        JButton excluir = Tema.botao("Excluir", false);
        excluir.setForeground(Tema.ALERTA);
        JButton atualizar = Tema.botao("Atualizar", false);
        novo.addActionListener(e -> novoMedicamento());
        repor.addActionListener(e -> reporEstoque());
        editar.addActionListener(e -> editarMedicamento());
        excluir.addActionListener(e -> excluirMedicamento());
        atualizar.addActionListener(e -> carregarDados());
        acoes.add(novo);
        acoes.add(repor);
        acoes.add(editar);
        acoes.add(excluir);
        acoes.add(atualizar);
        return criarPagina("Meus medicamentos", "Acompanhe a autonomia e organize a próxima compra com tranquilidade.", tabela, acoes);
    }

    private JPanel criarAbaFamilia() {
        JPanel cards = new JPanel();
        cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
        cards.setOpaque(false);

        JPanel acesso = Tema.card(new BorderLayout(0, 12));
        acesso.add(Tema.texto("Responsáveis autorizados", 16, true, Tema.TEXTO), BorderLayout.NORTH);
        listaResponsaveis = new JPanel(new GridLayout(0, 1, 0, 8));
        listaResponsaveis.setOpaque(false);
        acesso.add(listaResponsaveis, BorderLayout.CENTER);
        JButton gerenciar = Tema.botao("Gerenciar acessos", true);
        gerenciar.addActionListener(e -> {
            ContaDialog.gerenciarAcessos(this, sessao);
            carregarResponsaveis();
        });
        acesso.add(gerenciar, BorderLayout.SOUTH);
        cards.add(acesso);
        cards.add(Box.createVerticalStrut(16));
        JPanel adesao = Tema.card(new BorderLayout(0, 14));
        adesao.add(Tema.texto("Um passo de cada vez", 16, true, Tema.TEXTO), BorderLayout.NORTH);
        lblAdesaoTexto = Tema.texto("Consultando doses de hoje...", 13, false, Tema.SUAVE);
        pgbAdesao = new JProgressBar(0, 100);
        pgbAdesao.setStringPainted(true);
        pgbAdesao.setPreferredSize(new Dimension(200, 22));
        pgbAdesao.setBorderPainted(false);
        adesao.add(lblAdesaoTexto, BorderLayout.CENTER);
        adesao.add(pgbAdesao, BorderLayout.SOUTH);
        cards.add(adesao);
        cards.add(Box.createVerticalStrut(16));
        cards.add(criarCard("Você está no controle",
                "<html>O estoque só diminui quando uma dose é confirmada.<br>" +
                "Os alertas de recompra ajudam sua família a se planejar antes que o medicamento acabe.</html>"));
        return criarPagina("Minha família", "Acompanhamento compartilhado para uma rotina com mais apoio.", Tema.rolagem(cards), null);
    }

    private void carregarResponsaveis() {
        listaResponsaveis.removeAll();
        try {
            List<Usuario> responsaveis = familiarDAO.listarResponsaveis(sessao);
            if (responsaveis.isEmpty()) {
                listaResponsaveis.add(Tema.texto("Nenhum responsável autorizado.", 13, false, Tema.SUAVE));
            }
            for (Usuario responsavel : responsaveis) {
                JLabel nome = Tema.texto("", 14, true, Tema.TEXTO);
                nome.putClientProperty("html.disable", true);
                nome.setText(responsavel.getNome());
                listaResponsaveis.add(nome);
            }
        } catch (SQLException e) {
            listaResponsaveis.add(Tema.texto("Não foi possível carregar os responsáveis.", 13, false, Tema.ALERTA));
        }
        listaResponsaveis.revalidate();
        listaResponsaveis.repaint();
    }

    private JPanel criarCard(String titulo, String htmlContent) {
        JPanel card = Tema.card(new BorderLayout(0, 12));
        card.add(Tema.texto(titulo, 16, true, Tema.TEXTO), BorderLayout.NORTH);
        card.add(Tema.texto(htmlContent, 13, false, Tema.SUAVE), BorderLayout.CENTER);
        return card;
    }

    private JPanel criarPainelRodape() {
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setOpaque(false);
        rodape.add(Tema.texto("Dados salvos neste computador", 11, false, Tema.SUAVE), BorderLayout.WEST);
        rodape.add(Tema.texto("Cuidado Diário  /  AEP 2026", 11, false, Tema.SUAVE), BorderLayout.EAST);
        return rodape;
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

                String status = alerta ? "Recomprar em breve" : "Estoque seguro";
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
                lblAlertaEstoque.setText(medicamentosEmAlerta + " para recomprar");
                lblAlertaEstoque.setForeground(Tema.ALERTA);
            } else {
                lblAlertaEstoque.setText("Tudo em dia");
                lblAlertaEstoque.setForeground(Tema.VERDE);
            }

            // 2. Carrega Doses
            List<Dose> doses = doseDAO.listarPorUsuario(pacienteAtivo.getId());
            doses.sort(java.util.Comparator.comparing(Dose::isStatusTomado)
                    .thenComparing(Dose::getHorarioPrevisto,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
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
                String sitStr = tomada ? "Confirmada" : "Aguardando confirmação";

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
                lblAlertaDoses.setText(dosesDevidasAgora + " dose(s) pendente(s)");
                lblAlertaDoses.setForeground(new Color(169, 107, 37));
            } else {
                lblAlertaDoses.setText("Nenhuma dose pendente");
                lblAlertaDoses.setForeground(Tema.VERDE);
            }

            // Atualiza barra de adesão familiar
            if (dosesHojeTotal > 0) {
                int pct = (int) Math.round(((double) dosesHojeTomadas / dosesHojeTotal) * 100.0);
                pgbAdesao.setValue(pct);
                lblResumoAdesao.setText(dosesHojeTomadas + " de " + dosesHojeTotal + " doses confirmadas");
                lblAdesaoTexto.setText(String.format("%d de %d doses de hoje já foram tomadas (%d%% de adesão).",
                        dosesHojeTomadas, dosesHojeTotal, pct));
            } else {
                pgbAdesao.setValue(0);
                lblResumoAdesao.setText("Sem doses para hoje");
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

        if (situacao != null && situacao.equals("Confirmada")) {
            JOptionPane.showMessageDialog(this,
                    "Esta dose já foi registrada como tomada anteriormente.",
                    "Dose já Confirmada", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Medicamento med = doseDAO.confirmarTomadaDose(doseId, pacienteAtivo.getId());
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

                List<Medicamento> atualizados = doseDAO.confirmarTodasDosesAgora(ids, pacienteAtivo.getId());
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
                doseDAO.cadastrarTratamento(novo, 6);
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
            Medicamento med = medicamentoDAO.buscarPorId(id, pacienteAtivo.getId());
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
            Medicamento med = medicamentoDAO.buscarPorId(id, pacienteAtivo.getId());
            if (med == null) return;

            ReporEstoqueDialog dlg = new ReporEstoqueDialog(this, med);
            dlg.setVisible(true);

            if (dlg.isConfirmado()) {
                med.adicionarEstoque(dlg.getQuantidadeAdicionada());
                int novaQtd = med.getQuantidadeEstoque();
                medicamentoDAO.atualizarEstoque(id, novaQtd, pacienteAtivo.getId());
                carregarDados();

                JOptionPane.showMessageDialog(this,
                        String.format("Estoque de '%s' atualizado!\nNova quantidade: %d comprimidos (Autonomia: %d dias).",
                                med.getNome(), novaQtd, med.calcularDiasRestantes()),
                        "Estoque Reposto com Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (SQLException | IllegalArgumentException e) {
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
                medicamentoDAO.excluir(id, pacienteAtivo.getId());
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
