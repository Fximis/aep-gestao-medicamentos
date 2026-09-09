package models;

import java.util.ArrayList;
import java.util.List;

public class Medicamento {
    private int id;
    private String nome;
    private int quantidadeEstoque;
    private int frequenciaHoras;
    private int quantidadePorDose;
    private List<Dose> doses; // Composição 1:N

    public Medicamento(int id, String nome, int quantidadeEstoque, int frequenciaHoras, int quantidadePorDose) {
        this.id = id;
        this.nome = nome;
        this.quantidadeEstoque = quantidadeEstoque;
        this.frequenciaHoras = frequenciaHoras;
        this.quantidadePorDose = quantidadePorDose;
        this.doses = new ArrayList<>();
    }

    public void adicionarDose(Dose dose) {
        this.doses.add(dose);
    }

    public void reduzirEstoque(int qtd) {
        if (this.quantidadeEstoque >= qtd) {
            this.quantidadeEstoque -= qtd;
        } else {
            System.out.println("Estoque insuficiente.");
        }
    }

    public int calcularDiasRestantes() {
        int dosesPorDia = 24 / frequenciaHoras;
        int consumoDiario = dosesPorDia * quantidadePorDose;
        if (consumoDiario == 0) return 0;
        return quantidadeEstoque / consumoDiario;
    }

    public boolean verificarAlertaRecompra() {
        return calcularDiasRestantes() <= 5;
    }

    public int getQuantidadePorDose() {
        return quantidadePorDose;
    }

    public String getNome() {
        return nome;
    }
}
