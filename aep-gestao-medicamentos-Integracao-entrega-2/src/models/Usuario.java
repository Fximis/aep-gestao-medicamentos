package models;

/** Identidade usada pela aplicação. As credenciais são tratadas na autenticação. */
public abstract class Usuario {
    private final int id;
    private final String nome;
    private final String email;

    protected Usuario(int id, String nome, String email) {
        this.id = id;
        this.nome = nome;
        this.email = email;
    }

    public abstract String obterTipo();

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return nome + " (" + email + ")";
    }
}
