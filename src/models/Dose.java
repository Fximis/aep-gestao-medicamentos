package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entidade representando o agendamento e o registro histórico de cada dose administrada.
 */
public class Dose {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private int medicamentoId;
    private String nomeMedicamento; // Auxiliar para exibição em relatórios
    private LocalDateTime horarioPrevisto;
    private LocalDateTime horarioConsumo;
    private boolean statusTomado;

    public Dose() {
    }

    public Dose(int id, int medicamentoId, LocalDateTime horarioPrevisto) {
        this.id = id;
        this.medicamentoId = medicamentoId;
        this.horarioPrevisto = horarioPrevisto;
        this.statusTomado = false;
    }

    public Dose(int id, int medicamentoId, String nomeMedicamento, LocalDateTime horarioPrevisto, LocalDateTime horarioConsumo, boolean statusTomado) {
        this.id = id;
        this.medicamentoId = medicamentoId;
        this.nomeMedicamento = nomeMedicamento;
        this.horarioPrevisto = horarioPrevisto;
        this.horarioConsumo = horarioConsumo;
        this.statusTomado = statusTomado;
    }

    /**
     * Marca formalmente a dose como consumida (RN01, RN06).
     */
    public void marcarComoTomada() {
        this.statusTomado = true;
        this.horarioConsumo = LocalDateTime.now();
    }

    /**
     * Retorna o horário previsto em formato amigável para pessoas comuns.
     * Ex: "Hoje às 14:00", "Amanhã às 08:00" ou "10/11/2026 14:00".
     */
    public String getHorarioPrevistoFormatado() {
        if (horarioPrevisto == null) {
            return "Não informado";
        }
        LocalDateTime agora = LocalDateTime.now();
        if (horarioPrevisto.toLocalDate().isEqual(agora.toLocalDate())) {
            return "Hoje às " + horarioPrevisto.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (horarioPrevisto.toLocalDate().isEqual(agora.toLocalDate().plusDays(1))) {
            return "Amanhã às " + horarioPrevisto.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (horarioPrevisto.toLocalDate().isEqual(agora.toLocalDate().minusDays(1))) {
            return "Ontem às " + horarioPrevisto.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return horarioPrevisto.format(FORMATTER);
    }

    /**
     * Retorna o horário real de consumo formatado.
     */
    public String getHorarioConsumoFormatado() {
        if (horarioConsumo == null) {
            return "Ainda não tomada";
        }
        LocalDateTime agora = LocalDateTime.now();
        if (horarioConsumo.toLocalDate().isEqual(agora.toLocalDate())) {
            return "Hoje às " + horarioConsumo.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        return horarioConsumo.format(FORMATTER);
    }

    // Getters e Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMedicamentoId() {
        return medicamentoId;
    }

    public void setMedicamentoId(int medicamentoId) {
        this.medicamentoId = medicamentoId;
    }

    public String getNomeMedicamento() {
        return nomeMedicamento;
    }

    public void setNomeMedicamento(String nomeMedicamento) {
        this.nomeMedicamento = nomeMedicamento;
    }

    public LocalDateTime getHorarioPrevisto() {
        return horarioPrevisto;
    }

    public void setHorarioPrevisto(LocalDateTime horarioPrevisto) {
        this.horarioPrevisto = horarioPrevisto;
    }

    public LocalDateTime getHorarioConsumo() {
        return horarioConsumo;
    }

    public void setHorarioConsumo(LocalDateTime horarioConsumo) {
        this.horarioConsumo = horarioConsumo;
    }

    public boolean isStatusTomado() {
        return statusTomado;
    }

    public void setStatusTomado(boolean statusTomado) {
        this.statusTomado = statusTomado;
    }

    @Override
    public String toString() {
        String previstoStr = horarioPrevisto != null ? horarioPrevisto.format(FORMATTER) : "N/D";
        String consumoStr = horarioConsumo != null ? horarioConsumo.format(FORMATTER) : "Pendente";
        String statusStr = statusTomado ? "[TOMADA em " + consumoStr + "]" : "[AGUARDANDO CONFIRMAÇÃO]";
        String medStr = (nomeMedicamento != null && !nomeMedicamento.isEmpty()) ? " | Medicamento: " + nomeMedicamento : "";

        return String.format("Dose #%d%s | Horário Previsto: %s | Status: %s",
                id, medStr, previstoStr, statusStr);
    }
}
