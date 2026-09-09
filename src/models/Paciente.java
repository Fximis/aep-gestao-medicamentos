package models;

import java.util.ArrayList;
import java.util.List;

public class Paciente extends Usuario {
    private String condicaoMedica;
    private List<Medicamento> medicamentos; // Composição 1:N

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
        this.medicamentos.add(medicamento);
    }

    public void confirmarDose(Dose dose) {
        dose.marcarComoTomada();
    }
}
