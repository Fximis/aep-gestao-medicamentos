import models.*;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {
        System.out.println("--- Sistema de Gestão de Medicamentos Contínuos ---");

        // 1. Criando Paciente e Responsável (Demonstrando Herança e Polimorfismo)
        Paciente paciente = new Paciente(1, "João Silva", "joao@email.com", "senha123", "Hipertensão");
        Responsavel responsavel = new Responsavel(2, "Maria Silva", "maria@email.com", "senha123", "Filha");

        System.out.println("Usuário Logado: " + paciente.getNome() + " | Tipo: " + paciente.obterTipo());
        System.out.println("Usuário Acompanhante: " + responsavel.getNome() + " | Tipo: " + responsavel.obterTipo());

        // 2. Criando Medicamento (Composição)
        // Nome, Quantidade inicial = 30, a cada 12h, 1 por dose
        Medicamento losartana = new Medicamento(101, "Losartana 50mg", 30, 12, 1);
        paciente.adicionarMedicamento(losartana);

        // 3. Criando Doses Previstas
        Dose doseManha = new Dose(1001, LocalDateTime.now().withHour(8).withMinute(0), losartana);
        Dose doseNoite = new Dose(1002, LocalDateTime.now().withHour(20).withMinute(0), losartana);
        losartana.adicionarDose(doseManha);
        losartana.adicionarDose(doseNoite);

        // 4. Simulando consumo para disparar regras de negócio
        System.out.println("\n--- Confirmando Tomada da Dose ---");
        paciente.confirmarDose(doseManha);
        
        System.out.println("Dias restantes de tratamento com " + losartana.getNome() + ": " + losartana.calcularDiasRestantes() + " dias.");

        // Forçando o estoque para baixo para simular alerta de 5 dias
        losartana.reduzirEstoque(19); // Sobram 10 comprimidos. 2 por dia = 5 dias.
        
        System.out.println("\n--- Confirmando Próxima Dose (Gatilho de Alerta) ---");
        paciente.confirmarDose(doseNoite);
    }
}
