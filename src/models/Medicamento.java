package models;

/**
 * Entidade central representando um Medicamento de uso contínuo.
 * Contém a modelagem de domínio, regras matemáticas de autonomia e gatilho de recompra.
 */
public class Medicamento {

    public static final int LIMITE_DIAS_ALERTA_RECOMPRA = 5;

    private int id;
    private int usuarioId;
    private String nome;
    private int quantidadeEstoque;
    private int frequenciaHoras;
    private int quantidadePorDose;

    public Medicamento() {
    }

    public Medicamento(int id, int usuarioId, String nome, int quantidadeEstoque, int frequenciaHoras, int quantidadePorDose) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.setQuantidadeEstoque(quantidadeEstoque);
        this.setFrequenciaHoras(frequenciaHoras);
        this.setQuantidadePorDose(quantidadePorDose);
    }

    public Medicamento(int usuarioId, String nome, int quantidadeEstoque, int frequenciaHoras, int quantidadePorDose) {
        this(0, usuarioId, nome, quantidadeEstoque, frequenciaHoras, quantidadePorDose);
    }

    /**
     * Calcula dinamicamente a autonomia do estoque em dias de tratamento (RN05).
     * Fórmula: Estoque Atual / ( (24 / Frequência em Horas) * Quantidade por Dose )
     */
    public int calcularDiasRestantes() {
        if (frequenciaHoras <= 0 || quantidadePorDose <= 0) {
            return 0;
        }
        double dosesPorDia = 24.0 / frequenciaHoras;
        double consumoDiario = dosesPorDia * quantidadePorDose;
        if (consumoDiario <= 0) {
            return 0;
        }
        return (int) (quantidadeEstoque / consumoDiario);
    }

    /**
     * Verifica se o estoque atual atingiu a margem crítica de recompra (RN03 e RF05).
     */
    public boolean verificarAlertaRecompra() {
        return calcularDiasRestantes() <= LIMITE_DIAS_ALERTA_RECOMPRA;
    }

    /**
     * Reduz do estoque físico a quantidade consumida (RN01, RN02 e RN04).
     */
    public boolean reduzirEstoque(int qtd) {
        if (qtd <= 0) {
            return false;
        }
        if (this.quantidadeEstoque >= qtd) {
            this.quantidadeEstoque -= qtd;
            return true;
        }
        return false;
    }

    /**
     * Ajuste manual de estoque decorrente de nova compra ou correção física (RF07 e RN07).
     */
    public void adicionarEstoque(int qtd) {
        if (qtd > 0) {
            this.quantidadeEstoque += qtd;
        }
    }

    // Getters e Setters com validação de regras de negócio
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(int quantidadeEstoque) {
        // RN04: O estoque de um medicamento nunca poderá assumir valores negativos.
        if (quantidadeEstoque < 0) {
            throw new IllegalArgumentException("O estoque não pode ser negativo.");
        }
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public int getFrequenciaHoras() {
        return frequenciaHoras;
    }

    public void setFrequenciaHoras(int frequenciaHoras) {
        if (frequenciaHoras <= 0) {
            throw new IllegalArgumentException("A frequência de doses em horas deve ser maior que zero.");
        }
        this.frequenciaHoras = frequenciaHoras;
    }

    public int getQuantidadePorDose() {
        return quantidadePorDose;
    }

    public void setQuantidadePorDose(int quantidadePorDose) {
        if (quantidadePorDose <= 0) {
            throw new IllegalArgumentException("A quantidade por dose deve ser de pelo menos 1 unidade.");
        }
        this.quantidadePorDose = quantidadePorDose;
    }

    @Override
    public String toString() {
        int dias = calcularDiasRestantes();
        String alerta = verificarAlertaRecompra() ? " [! ALERTA: RECOMPRA NECESSARIA]" : " [OK]";
        return String.format("ID: %d | Medicamento: %-25s | Estoque: %3d comp | De %2dh em %2dh (Dose: %d) | Autonomia: %2d dias%s",
                id, nome, quantidadeEstoque, frequenciaHoras, frequenciaHoras, quantidadePorDose, dias, alerta);
    }
}
