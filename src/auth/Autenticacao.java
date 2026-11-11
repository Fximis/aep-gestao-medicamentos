package auth;

import dao.UsuarioDAO;
import models.Paciente;
import models.Responsavel;
import models.Usuario;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/** Cadastro, autenticação e política de senha usados por Swing e console. */
public final class Autenticacao {
    public enum Perfil {
        PACIENTE("Paciente"), RESPONSAVEL("Responsável");
        private final String rotulo;
        Perfil(String rotulo) { this.rotulo = rotulo; }
        @Override public String toString() { return rotulo; }
    }

    public static final class Sessao {
        private final Usuario usuario;
        private boolean ativa = true;

        private Sessao(Usuario usuario) { this.usuario = usuario; }
        public Usuario usuario() {
            if (!ativa) throw new IllegalStateException("Sessão encerrada.");
            return usuario;
        }
        public void encerrar() { ativa = false; }
    }

    private static final Pattern EMAIL = Pattern.compile("[a-z0-9.!#$%&'*+/=?^_{}|~-]+@[a-z0-9-]+(?:\\.[a-z0-9-]+)+");
    private static final int MAX_TENTATIVAS = 5;
    private static final int LIMITE_SENHA_LEGADA = 255;
    private static final long PAUSA = Duration.ofSeconds(30).toNanos();

    private final UsuarioDAO usuarios = new UsuarioDAO();
    private int falhas;
    private long bloqueadoAte;

    public void cadastrar(String nome, String email, Perfil perfil, String detalhe,
                          char[] senha, char[] confirmacao) throws SQLException {
        String nomeLimpo = validarTexto(nome, 100, "Nome", true);
        String emailLimpo = validarEmail(email);
        Objects.requireNonNull(perfil, "Informe o perfil.");
        String detalheLimpo = validarTexto(detalhe, perfil == Perfil.PACIENTE ? 255 : 100,
                perfil == Perfil.PACIENTE ? "Condição médica" : "Vínculo familiar", false);
        validarNovaSenha(senha, confirmacao);
        Usuario usuario = perfil == Perfil.PACIENTE
                ? new Paciente(nomeLimpo, emailLimpo, detalheLimpo)
                : new Responsavel(nomeLimpo, emailLimpo, detalheLimpo);
        usuarios.inserir(usuario, Senhas.gerar(senha));
    }

    public synchronized Sessao entrar(String email, char[] senha) throws SQLException {
        verificarLimite();
        Usuario usuario = null;
        String normalizado = UsuarioDAO.normalizarEmail(email);
        if (normalizado.length() <= 100 && senha.length <= LIMITE_SENHA_LEGADA) {
            usuario = usuarios.autenticar(normalizado, senha);
        }
        if (usuario == null) {
            registrarFalha();
            throw new IllegalArgumentException("E-mail ou senha inválidos.");
        }
        falhas = 0;
        return new Sessao(usuario);
    }

    public synchronized void alterarSenha(Sessao sessao, char[] atual, char[] nova, char[] confirmacao)
            throws SQLException {
        verificarLimite();
        int id = sessao.usuario().getId();
        validarNovaSenha(nova, confirmacao);
        if (atual.length > LIMITE_SENHA_LEGADA || !usuarios.alterarSenha(id, atual, nova)) {
            registrarFalha();
            throw new IllegalArgumentException("Não foi possível confirmar a senha atual.");
        }
        falhas = 0;
    }

    public static String validarEmail(String email) {
        String valor = UsuarioDAO.normalizarEmail(email);
        if (valor.length() > 100 || !EMAIL.matcher(valor).matches()
                || valor.startsWith(".") || valor.contains("..") || valor.contains(".@")) {
            throw new IllegalArgumentException("Informe um e-mail válido de até 100 caracteres.");
        }
        return valor;
    }

    private static String validarTexto(String texto, int maximo, String campo, boolean obrigatorio) {
        String valor = texto.strip();
        if ((obrigatorio && valor.isEmpty()) || valor.length() > maximo
                || valor.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(campo + " inválido. Use até " + maximo + " caracteres.");
        }
        return valor;
    }

    private static void validarNovaSenha(char[] senha, char[] confirmacao) {
        if (senha.length < 15 || senha.length > 128 || !temConteudo(senha)) {
            throw new IllegalArgumentException("Escolha uma senha de 15 a 128 caracteres.");
        }
        if (!Arrays.equals(senha, confirmacao)) throw new IllegalArgumentException("As senhas não coincidem.");
    }

    private static boolean temConteudo(char[] senha) {
        for (char caractere : senha) {
            if (!Character.isWhitespace(caractere)) return true;
        }
        return false;
    }

    private void verificarLimite() {
        if (falhas < MAX_TENTATIVAS) return;
        if (bloqueadoAte - System.nanoTime() > 0) {
            throw new IllegalStateException("Muitas tentativas. Aguarde até 30 segundos e tente novamente.");
        }
        falhas = 0;
    }

    private void registrarFalha() {
        if (++falhas == MAX_TENTATIVAS) bloqueadoAte = System.nanoTime() + PAUSA;
    }
}
