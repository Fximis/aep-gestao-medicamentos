package models;

import java.time.LocalDateTime;

public class Dose {
    private int id;
    private LocalDateTime horarioPrevisto;
    private LocalDateTime horarioConsumo;
    private boolean statusTomado;
    private Medicamento medicamentoAssociado; // Referência para a composição

    public Dose(int id, LocalDateTime horarioPrevisto, Medicamento medicamentoAssociado) {
        this.id = id;
        this.horarioPrevisto = horarioPrevisto;
        this.statusTomado = false;
        this.medicamentoAssociado = medicamentoAssociado;
    }

    public void marcarComoTomada() {
        if (!this.statusTomado) {
            this.statusTomado = true;
            this.horarioConsumo = LocalDateTime.now();
            // Deduz do estoque do medicamento associado
            this.medicamentoAssociado.reduzirEstoque(this.medicamentoAssociado.getQuantidadePorDose());
            System.out.println("Dose de " + this.medicamentoAssociado.getNome() + " confirmada.");
            
            if (this.medicamentoAssociado.verificarAlertaRecompra()) {
                System.out.println("ALERTA: O medicamento " + this.medicamentoAssociado.getNome() + " tem estoque para 5 dias ou menos!");
            }
        } else {
            System.out.println("Esta dose já foi registrada como tomada.");
        }
    }
}
