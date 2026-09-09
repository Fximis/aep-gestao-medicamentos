package models;

public class Responsavel extends Usuario {
    private String vinculoFamiliar;

    public Responsavel(int id, String nome, String email, String senha, String vinculoFamiliar) {
        super(id, nome, email, senha);
        this.vinculoFamiliar = vinculoFamiliar;
    }

    @Override
    public String obterTipo() {
        return "RESPONSAVEL";
    }

    public void monitorarEstoque(Paciente paciente) {
        System.out.println("Monitorando estoque do paciente: " + paciente.getNome());
        // Lógica para listar os medicamentos e níveis de estoque do paciente associado.
    }
}
