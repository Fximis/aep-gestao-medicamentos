package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Subclasse representando o Paciente que realiza o tratamento contínuo.
 * Demonstra Herança (extends Usuario) e Polimorfismo (@Override obterTipo).
 */
public class Paciente extends Usuario {

    private String condicaoMedica;
    private final List<Medicamento> medicamentos;

    public Paciente(int id, String nome, String email, String condicaoMedica) {
        super(id, nome, email);
        this.condicaoMedica = condicaoMedica;
        this.medicamentos = new ArrayList<>();
    }

    public Paciente(String nome, String email, String condicaoMedica) {
        this(0, nome, email, condicaoMedica);
    }

    @Override
    public String obterTipo() {
        return "PACIENTE";
    }

    public void adicionarMedicamento(Medicamento medicamento) {
        if (medicamento == null) {
            throw new IllegalArgumentException("O medicamento não pode ser nulo.");
        }
        this.medicamentos.add(medicamento);
    }

    public String getCondicaoMedica() {
        return condicaoMedica;
    }

    public void setCondicaoMedica(String condicaoMedica) {
        this.condicaoMedica = condicaoMedica;
    }

    public List<Medicamento> getMedicamentos() {
        // Protege a estrutura da coleção; as entidades mantêm seus próprios setters.
        return Collections.unmodifiableList(new ArrayList<>(medicamentos));
    }

    public void setMedicamentos(List<Medicamento> medicamentos) {
        if (medicamentos == null || medicamentos.stream().anyMatch(m -> m == null)) {
            throw new IllegalArgumentException("Informe uma lista de medicamentos sem itens nulos.");
        }
        List<Medicamento> copia = new ArrayList<>(medicamentos);
        this.medicamentos.clear();
        this.medicamentos.addAll(copia);
    }

    @Override
    public String toString() {
        return super.toString() + " | Condicao Medica: " + condicaoMedica;
    }
}
