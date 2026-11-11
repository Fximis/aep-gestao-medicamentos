package models;

/**
 * Subclasse representando o Responsável/Cuidador familiar.
 * Demonstra Herança (extends Usuario) e Polimorfismo (@Override obterTipo).
 */
public class Responsavel extends Usuario {

    private String vinculoFamiliar;

    public Responsavel(int id, String nome, String email, String vinculoFamiliar) {
        super(id, nome, email);
        this.vinculoFamiliar = vinculoFamiliar;
    }

    public Responsavel(String nome, String email, String vinculoFamiliar) {
        this(0, nome, email, vinculoFamiliar);
    }

    @Override
    public String obterTipo() {
        return "RESPONSAVEL";
    }

    public String getVinculoFamiliar() {
        return vinculoFamiliar;
    }

    public void setVinculoFamiliar(String vinculoFamiliar) {
        this.vinculoFamiliar = vinculoFamiliar;
    }

    @Override
    public String toString() {
        return super.toString() + " | Vinculo Familiar: " + vinculoFamiliar;
    }
}
