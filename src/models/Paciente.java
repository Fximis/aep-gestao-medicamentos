package models;

import java.util.ArrayList;
import java.util.List;

/**
 * Subclasse representando o Paciente que realiza o tratamento contínuo.
 * Demonstra Herança (extends Usuario) e Polimorfismo (@Override obterTipo).
 */
public class Paciente extends Usuario {

    private String condicaoMedica;
    private List<Medicamento> medicamentos;

    public Paciente(int id, String nome, String email, String senha, String condicaoMedica) {
        super(id, nome, email, senha);
        this.condicaoMedica = condicaoMedica;
        this.medicamentos = new ArrayList<>();
    }

    @Override
    public String obterTipo() {
        return "PACIENTE";
    }

    public void adicionarMedicamento(Medicamento medicamento) {
        if (medicamento != null) {
            this.medicamentos.add(medicamento);
        }
    }

    public String getCondicaoMedica() {
        return condicaoMedica;
    }

    public void setCondicaoMedica(String condicaoMedica) {
        this.condicaoMedica = condicaoMedica;
    }

    public List<Medicamento> getMedicamentos() {
        return medicamentos;
    }

    public void setMedicamentos(List<Medicamento> medicamentos) {
        this.medicamentos = medicamentos;
    }

    @Override
    public String toString() {
        return super.toString() + " | Condicao Medica: " + condicaoMedica;
    }
}
