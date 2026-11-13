package auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** Representação versionada de senhas com salt aleatório por conta. */
public final class Senhas {
    private static final String PREFIXO = "pbkdf2-sha256$600000$";
    private static final int ITERACOES = 600_000;
    private static final int BYTES_SALT = 16;
    private static final int BYTES_CHAVE = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Base64.Encoder CODIFICADOR = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICADOR = Base64.getDecoder();

    private Senhas() { }

    public static String gerar(char[] senha) {
        byte[] salt = new byte[BYTES_SALT];
        ALEATORIO.nextBytes(salt);
        byte[] chave = derivar(senha, salt);
        try {
            return PREFIXO + CODIFICADOR.encodeToString(salt) + "$" + CODIFICADOR.encodeToString(chave);
        } finally {
            Arrays.fill(chave, (byte) 0);
        }
    }

    public static boolean verificar(char[] senha, String hash) {
        byte[][] partes = decodificar(hash);
        // Mantém uma derivação também quando o e-mail está ausente ou a conta está desativada.
        byte[] salt = partes == null ? new byte[BYTES_SALT] : partes[0];
        byte[] esperada = partes == null ? new byte[BYTES_CHAVE] : partes[1];
        byte[] calculada = derivar(senha, salt);
        try {
            boolean igual = MessageDigest.isEqual(esperada, calculada);
            return partes != null && igual;
        } finally {
            Arrays.fill(calculada, (byte) 0);
            Arrays.fill(esperada, (byte) 0);
        }
    }

    public static boolean ehHash(String valor) {
        byte[][] partes = decodificar(valor);
        if (partes == null) return false;
        Arrays.fill(partes[1], (byte) 0);
        return true;
    }

    private static byte[][] decodificar(String valor) {
        if (valor == null || !valor.startsWith(PREFIXO)
                || valor.length() != PREFIXO.length() + 22 + 1 + 43) return null;
        int separador = PREFIXO.length() + 22;
        if (valor.charAt(separador) != '$') return null;
        try {
            byte[] salt = DECODIFICADOR.decode(valor.substring(PREFIXO.length(), separador));
            byte[] chave = DECODIFICADOR.decode(valor.substring(separador + 1));
            return salt.length == BYTES_SALT && chave.length == BYTES_CHAVE ? new byte[][]{salt, chave} : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] derivar(char[] senha, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(senha, salt, ITERACOES, BYTES_CHAVE * Byte.SIZE);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível proteger a senha.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
