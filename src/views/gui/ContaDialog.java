package views.gui;

import auth.Autenticacao;
import dao.FamiliarDAO;
import models.Usuario;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.Arrays;

/** Ações de conta e consentimento do paciente. */
final class ContaDialog {
    private ContaDialog() { }

    static void alterarSenha(JFrame janela, Autenticacao autenticacao, Autenticacao.Sessao sessao) {
        JPanel form = LoginWindow.formulario();
        JPasswordField atual = new JPasswordField(24);
        JPasswordField nova = new JPasswordField(24);
        JPasswordField confirmacao = new JPasswordField(24);
        LoginWindow.campo(form, "Senha atual", atual);
        LoginWindow.campo(form, "Nova senha (" + Autenticacao.FAIXA_SENHA + ")", nova);
        LoginWindow.campo(form, "Confirme a nova senha", confirmacao);
        if (JOptionPane.showConfirmDialog(janela, form, "Alterar senha",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            atual.setText("");
            nova.setText("");
            confirmacao.setText("");
            return;
        }
        char[] anterior = atual.getPassword();
        char[] primeira = nova.getPassword();
        char[] segunda = confirmacao.getPassword();
        atual.setText("");
        nova.setText("");
        confirmacao.setText("");
        String mensagemErro = null;
        try {
            autenticacao.alterarSenha(sessao, anterior, primeira, segunda);
        } catch (IllegalArgumentException | IllegalStateException e) {
            mensagemErro = e.getMessage();
        } catch (SQLException e) {
            mensagemErro = "Não foi possível atualizar a senha.";
        } finally {
            Arrays.fill(anterior, '\0');
            Arrays.fill(primeira, '\0');
            Arrays.fill(segunda, '\0');
        }
        if (mensagemErro == null) JOptionPane.showMessageDialog(janela, "Senha atualizada.");
        else erro(janela, mensagemErro);
    }

    static void gerenciarAcessos(JFrame janela, Autenticacao.Sessao sessao) {
        FamiliarDAO familia = new FamiliarDAO();
        JDialog dialog = new JDialog(janela, "Acesso familiar", true);
        DefaultListModel<Usuario> modelo = new DefaultListModel<>();
        JList<Usuario> responsaveis = new JList<>(modelo);
        responsaveis.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        responsaveis.setCellRenderer(renderizadorPerfil());
        JTextField email = new JTextField(24);
        JButton conceder = Tema.botao("Autorizar consulta", true);
        JButton revogar = Tema.botao("Revogar selecionado", false);
        JPanel form = LoginWindow.formulario();
        LoginWindow.campo(form, "E-mail da conta do responsável", email);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        acoes.add(conceder);
        acoes.add(revogar);
        form.add(acoes);
        JPanel conteudo = new JPanel(new BorderLayout(8, 8));
        conteudo.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        conteudo.add(Tema.texto("Responsáveis com acesso de leitura", 15, true, Tema.TEXTO), BorderLayout.NORTH);
        conteudo.add(new JScrollPane(responsaveis), BorderLayout.CENTER);
        conteudo.add(form, BorderLayout.SOUTH);
        Runnable atualizar = () -> {
            modelo.clear();
            try {
                modelo.addAll(familia.listarResponsaveis(sessao));
            } catch (SQLException e) {
                erro(dialog, "Não foi possível consultar os acessos.");
            }
        };
        conceder.addActionListener(e -> {
            try {
                familia.autorizar(sessao, email.getText());
                email.setText("");
                atualizar.run();
            } catch (IllegalArgumentException e1) {
                erro(dialog, e1.getMessage());
            } catch (SQLException e1) {
                erro(dialog, "Não foi possível conceder o acesso.");
            }
        });
        revogar.addActionListener(e -> {
            Usuario selecionado = responsaveis.getSelectedValue();
            if (selecionado == null) {
                erro(dialog, "Selecione um responsável na lista.");
                return;
            }
            try {
                familia.revogar(sessao, selecionado.getId());
                atualizar.run();
            } catch (SQLException e1) {
                erro(dialog, "Não foi possível revogar o acesso.");
            }
        });
        dialog.setContentPane(conteudo);
        dialog.setSize(520, 360);
        dialog.setLocationRelativeTo(janela);
        atualizar.run();
        dialog.setVisible(true);
        dialog.dispose();
    }

    static DefaultListCellRenderer renderizadorPerfil() {
        return new DefaultListCellRenderer() {
            { putClientProperty("html.disable", true); }

            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                          boolean selecionado, boolean foco) {
                Object rotulo = valor instanceof Usuario usuario
                        ? usuario.getNome() + " (" + usuario.getEmail() + ")" : valor;
                return super.getListCellRendererComponent(lista, rotulo, indice, selecionado, foco);
            }
        };
    }

    static void sair(JFrame janela, Autenticacao autenticacao, Autenticacao.Sessao sessao) {
        sessao.encerrar();
        janela.dispose();
        new LoginWindow(autenticacao).setVisible(true);
    }

    private static void erro(Component janela, String texto) {
        JOptionPane.showMessageDialog(janela, texto, "Cuidado Diário", JOptionPane.WARNING_MESSAGE);
    }
}
