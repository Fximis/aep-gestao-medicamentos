package views.gui;

import auth.Autenticacao;
import dao.FamiliarDAO;
import models.Dose;
import models.Medicamento;
import models.Responsavel;
import models.Usuario;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

/** Área exclusiva de consulta para responsáveis autorizados. */
final class ResponsavelWindow extends JFrame {
    private final Autenticacao.Sessao sessao;
    private final FamiliarDAO familia = new FamiliarDAO();
    private final JComboBox<Usuario> pacientes = new JComboBox<>();
    private final JTextArea dados = new JTextArea(16, 60);

    ResponsavelWindow(Autenticacao.Sessao sessao, Autenticacao autenticacao) {
        super("Cuidado Diário | Acompanhamento familiar");
        if (!(sessao.usuario() instanceof Responsavel)) throw new IllegalArgumentException("Perfil inválido.");
        this.sessao = sessao;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel tela = new JPanel(new BorderLayout(0, 16));
        tela.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JPanel topo = new JPanel();
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        topo.add(Tema.texto("Acompanhamento familiar", 24, true, Tema.TEXTO));
        topo.add(Box.createVerticalStrut(8));
        topo.add(Tema.texto("Responsável: " + sessao.usuario().getNome(), 13, false, Tema.SUAVE));
        topo.add(Box.createVerticalStrut(16));
        pacientes.setRenderer(ContaDialog.renderizadorPerfil());
        LoginWindow.campo(topo, "Paciente que autorizou a consulta", pacientes);
        tela.add(topo, BorderLayout.NORTH);
        dados.setEditable(false);
        dados.setLineWrap(true);
        dados.setWrapStyleWord(true);
        dados.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tela.add(Tema.rolagem(dados), BorderLayout.CENTER);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        JButton consultar = Tema.botao("Consultar", true);
        JButton atualizar = Tema.botao("Atualizar acessos", false);
        JButton senha = Tema.botao("Alterar senha", false);
        JButton sair = Tema.botao("Sair da conta", false);
        consultar.addActionListener(e -> consultar());
        atualizar.addActionListener(e -> carregarPacientes());
        senha.addActionListener(e -> ContaDialog.alterarSenha(this, autenticacao, sessao));
        sair.addActionListener(e -> ContaDialog.sair(this, autenticacao, sessao));
        acoes.add(consultar);
        acoes.add(atualizar);
        acoes.add(senha);
        acoes.add(sair);
        tela.add(acoes, BorderLayout.SOUTH);
        pacientes.addActionListener(e -> dados.setText(""));
        setContentPane(tela);
        pack();
        setMinimumSize(new Dimension(680, 480));
        setLocationRelativeTo(null);
        carregarPacientes();
    }

    private void carregarPacientes() {
        pacientes.removeAllItems();
        try {
            for (Usuario paciente : familia.listarPacientes(sessao)) pacientes.addItem(paciente);
            dados.setText(pacientes.getItemCount() == 0
                    ? "Nenhum paciente autorizou o acompanhamento." : "Selecione um paciente e clique em Consultar.");
        } catch (SQLException e) {
            dados.setText("Não foi possível consultar os acessos.");
        }
    }

    private void consultar() {
        dados.setText("");
        Usuario paciente = (Usuario) pacientes.getSelectedItem();
        if (paciente == null) {
            dados.setText("Nenhum paciente selecionado.");
            return;
        }
        try {
            FamiliarDAO.Acompanhamento resumo = familia.consultar(sessao, paciente.getId());
            StringBuilder texto = new StringBuilder("Paciente: ").append(resumo.paciente().getNome());
            texto.append("\n\nMedicamentos e estoque\n");
            if (resumo.medicamentos().isEmpty()) texto.append("Nenhum medicamento cadastrado.\n");
            for (Medicamento medicamento : resumo.medicamentos()) {
                texto.append(medicamento.getNome()).append(" | Estoque: ").append(medicamento.getQuantidadeEstoque())
                        .append(" | Autonomia: ").append(medicamento.calcularDiasRestantes()).append(" dia(s)");
                if (medicamento.verificarAlertaRecompra()) texto.append(" | Recomprar em breve");
                texto.append('\n');
            }
            texto.append("\nHorários e histórico de doses\n");
            if (resumo.doses().isEmpty()) texto.append("Nenhuma dose registrada.\n");
            for (Dose dose : resumo.doses()) {
                texto.append(dose.getNomeMedicamento()).append(" | ").append(dose.getHorarioPrevistoFormatado());
                if (dose.isStatusTomado()) texto.append(" | Confirmada ").append(dose.getHorarioConsumoFormatado());
                else texto.append(" | Pendente");
                texto.append('\n');
            }
            dados.setText(texto.toString());
            dados.setCaretPosition(0);
        } catch (IllegalArgumentException e) {
            carregarPacientes();
            dados.setText(e.getMessage());
        } catch (SQLException e) {
            dados.setText("Não foi possível consultar o acompanhamento.");
        }
    }
}
