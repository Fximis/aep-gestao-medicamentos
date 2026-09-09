package models;

public abstract class Usuario {
    protected int id;
    protected String nome;
    protected String email;
    protected String senha;

    public Usuario(int id, String nome, String email, String senha) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    public boolean autenticar(String email, String senha) {
        return this.email.equals(email) && this.senha.equals(senha);
    }

    // Método polimórfico
    public abstract String obterTipo();

    // Getters and Setters omitted for brevity
    public int getId() { return id; }
    public String getNome() { return nome; }
}
