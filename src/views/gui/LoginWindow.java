package views.gui;

import auth.Autenticacao;
import models.Paciente;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Entrada da aplicação. O painel só é construído depois da autenticação. */
public final class LoginWindow extends JFrame {
    private final Autenticacao autenticacao;
    private final JTabbedPane abas = new JTabbedPane() {
        @Override public Dimension getPreferredSize() {
            Component pagina = getSelectedComponent();
            if (pagina == null) return super.getPreferredSize();
            Dimension tamanho = pagina.getPreferredSize();
            return new Dimension(Math.max(440, tamanho.width + 12), tamanho.height + 34);
        }
    };
    private final JTextField emailLogin = new JTextField(28);
    private final JPasswordField senhaLogin = new JPasswordField(28);
    private final JTextArea estado = new JTextArea(2, 30);
    private final JButton entrar = Tema.botao("Entrar", true);
    private final JButton cadastrar = Tema.botao("Criar conta", true);

    public LoginWindow(Autenticacao autenticacao) {
        super("Cuidado Diário | Acesso");
        this.autenticacao = autenticacao;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel tela = new JPanel(new BorderLayout(0, 18));
        tela.setBackground(Tema.FUNDO);
        tela.setBorder(BorderFactory.createEmptyBorder(24, 26, 20, 26));
        JPanel cabecalho = new JPanel();
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));
        cabecalho.setOpaque(false);
        cabecalho.add(Tema.texto("Cuidado Diário", 27, true, Tema.TEXTO));
        cabecalho.add(Box.createVerticalStrut(6));
        cabecalho.add(Tema.texto("Gestão pessoal e acompanhamento familiar", 13, false, Tema.SUAVE));
        tela.add(cabecalho, BorderLayout.NORTH);
        abas.addTab("Entrar", criarLogin());
        abas.addTab("Cadastro", criarCadastro());
        abas.addChangeListener(e -> {
            getRootPane().setDefaultButton(abas.getSelectedIndex() == 0 ? entrar : cadastrar);
            setMinimumSize(new Dimension());
            pack();
            setMinimumSize(getSize());
            if (isShowing()) setLocationRelativeTo(null);
        });
        tela.add(abas, BorderLayout.CENTER);
        estado.setFont(new Font("SansSerif", Font.PLAIN, 12));
        estado.setEditable(false);
        estado.setFocusable(false);
        estado.setOpaque(false);
        estado.setLineWrap(true);
        estado.setWrapStyleWord(true);
        tela.add(estado, BorderLayout.SOUTH);
        setContentPane(tela);
        getRootPane().setDefaultButton(entrar);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private JPanel criarLogin() {
        JPanel form = formulario();
        campo(form, "E-mail", emailLogin);
        campo(form, "Senha", senhaLogin);
        entrar.addActionListener(e -> entrar());
        acao(form, entrar);
        return alinharAcima(form);
    }

    private JPanel criarCadastro() {
        JPanel form = formulario();
        JTextField nome = new JTextField(28);
        JTextField email = new JTextField(28);
        JComboBox<Autenticacao.Perfil> perfil = new JComboBox<>(Autenticacao.Perfil.values());
        JTextField detalhe = new JTextField(28);
        JPasswordField senha = new JPasswordField(28);
        JPasswordField confirmacao = new JPasswordField(28);
        campo(form, "Nome completo", nome);
        campo(form, "E-mail", email);
        campo(form, "Perfil", perfil);
        JLabel rotuloDetalhe = campo(form, "Condição médica (opcional)", detalhe);
        perfil.addActionListener(e -> rotuloDetalhe.setText(perfil.getSelectedItem() == Autenticacao.Perfil.PACIENTE
                ? "Condição médica (opcional)" : "Vínculo familiar (opcional)"));
        campo(form, "Senha (15 a 128 caracteres)", senha);
        campo(form, "Confirme a senha", confirmacao);
        cadastrar.addActionListener(e -> {
            char[] primeira = senha.getPassword();
            char[] segunda = confirmacao.getPassword();
            senha.setText("");
            confirmacao.setText("");
            String nomeConta = nome.getText();
            String emailConta = email.getText();
            String detalheConta = detalhe.getText();
            Autenticacao.Perfil tipo = (Autenticacao.Perfil) perfil.getSelectedItem();
            executar(() -> {
                try {
                    autenticacao.cadastrar(nomeConta, emailConta, tipo, detalheConta, primeira, segunda);
                    return null;
                } finally {
                    Arrays.fill(primeira, '\0');
                    Arrays.fill(segunda, '\0');
                }
            }, resultado -> {
                emailLogin.setText(emailConta.strip());
                nome.setText("");
                email.setText("");
                detalhe.setText("");
                abas.setSelectedIndex(0);
                senhaLogin.requestFocusInWindow();
                mensagem("Conta criada. Entre com seu e-mail e senha.", false);
            });
        });
        acao(form, cadastrar);
        return alinharAcima(form);
    }

    private void entrar() {
        String email = emailLogin.getText();
        char[] senha = senhaLogin.getPassword();
        senhaLogin.setText("");
        executar(() -> {
            try {
                return autenticacao.entrar(email, senha);
            } finally {
                Arrays.fill(senha, '\0');
            }
        }, sessao -> {
            JFrame painel = sessao.usuario() instanceof Paciente
                    ? new MainWindow(sessao, autenticacao) : new ResponsavelWindow(sessao, autenticacao);
            painel.setVisible(true);
            dispose();
        });
    }

    private <T> void executar(Callable<T> tarefa, Consumer<T> sucesso) {
        entrar.setEnabled(false);
        cadastrar.setEnabled(false);
        mensagem("Aguarde...", false);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return tarefa.call(); }

            @Override protected void done() {
                try {
                    sucesso.accept(get());
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    mensagem(causa instanceof IllegalArgumentException || causa instanceof IllegalStateException
                            ? causa.getMessage() : "Não foi possível acessar o banco. Tente novamente.", true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mensagem("Operação interrompida.", true);
                } finally {
                    entrar.setEnabled(true);
                    cadastrar.setEnabled(true);
                }
            }
        }.execute();
    }

    private void mensagem(String texto, boolean erro) {
        estado.setForeground(erro ? Tema.ALERTA : Tema.VERDE);
        estado.setText(texto);
    }

    static JPanel formulario() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        return form;
    }

    static JLabel campo(JPanel painel, String titulo, JComponent componente) {
        JLabel label = Tema.texto(titulo, 12, true, Tema.TEXTO);
        label.setLabelFor(componente);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        componente.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension tamanho = componente.getPreferredSize();
        componente.setPreferredSize(new Dimension(tamanho.width, 32));
        componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        painel.add(label);
        painel.add(Box.createVerticalStrut(4));
        painel.add(componente);
        painel.add(Box.createVerticalStrut(10));
        return label;
    }

    private static void acao(JPanel form, JButton botao) {
        botao.setAlignmentX(Component.LEFT_ALIGNMENT);
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, botao.getPreferredSize().height));
        form.add(botao);
    }

    private static JPanel alinharAcima(JPanel formulario) {
        JPanel painel = new JPanel(new BorderLayout());
        painel.add(formulario, BorderLayout.NORTH);
        return painel;
    }
}
