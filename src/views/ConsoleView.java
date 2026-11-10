package views;

import models.Dose;
import models.Medicamento;
import models.Paciente;
import models.Responsavel;
import models.Usuario;

import java.util.List;
import java.util.Scanner;

/**
 * Camada de Visualização em Console CLI (Terminal).
 * Oferece interface interativa e amigável para demonstração dos requisitos ao avaliador.
 */
public class ConsoleView {

    private final Scanner scanner;

    public ConsoleView() {
        this.scanner = new Scanner(System.in);
    }

    public void exibirCabecalho(Usuario usuarioAtivo) {
        System.out.println("\n" + "=".repeat(75));
        System.out.println("   SISTEMA DE GESTÃO DE MEDICAMENTOS CONTÍNUOS E ESTOQUE FAMILIAR");
        System.out.println("   Atividade de Estudo Programada (AEP) - 4º Semestre (2026.2)");
        System.out.println("=".repeat(75));
        if (usuarioAtivo != null) {
            System.out.printf("   Usuário Conectado: %s (%s) | Tipo: %s\n",
                    usuarioAtivo.getNome(), usuarioAtivo.getEmail(), usuarioAtivo.obterTipo());
        }
        System.out.println("-".repeat(75));
    }

    public int exibirMenuPrincipal() {
        System.out.println("\n--- OPERAÇÕES DE CRUD (ENTIDADE PRINCIPAL: MEDICAMENTO) ---");
        System.out.println("  [1] Cadastrar Novo Medicamento (Create)");
        System.out.println("  [2] Listar Medicamentos e Calcular Autonomia (Read)");
        System.out.println("  [3] Atualizar Dados do Medicamento (Update)");
        System.out.println("  [4] Ajustar Estoque Manualmente - Nova Compra (Update de Estoque)");
        System.out.println("  [5] Excluir Medicamento (Delete)");
        System.out.println("\n--- REGRAS DE NEGÓCIO E CICLO DE DOSES ---");
        System.out.println("  [6] Agenda de Doses do Paciente (RF02 - Lembretes)");
        System.out.println("  [7] Confirmar Tomada de Dose (RF03/04 - Baixa e Alerta de 5 Dias)");
        System.out.println("  [8] Painel do Responsável / Cuidador (RF06 - Gestão Familiar)");
        System.out.println("  [9] Demonstrar Polimorfismo e Hierarquia de Usuários (POO)");
        System.out.println("\n  [0] Encerrar Aplicação");
        System.out.print("\nEscolha uma opção: ");

        return lerInteiro();
    }

    public void exibirListaMedicamentos(List<Medicamento> medicamentos) {
        System.out.println("\n--- LISTAGEM DE MEDICAMENTOS CADASTRADOS ---");
        if (medicamentos == null || medicamentos.isEmpty()) {
            System.out.println("Nenhum medicamento encontrado no banco de dados.");
            return;
        }

        System.out.printf("%-5s | %-28s | %-12s | %-14s | %-16s | %s\n",
                "ID", "Nome Comercial", "Estoque Fís.", "Frequência", "Autonomia (Dias)", "Status");
        System.out.println("-".repeat(95));

        for (Medicamento m : medicamentos) {
            int dias = m.calcularDiasRestantes();
            String status = m.verificarAlertaRecompra() ? "🚨 RECOMPRA URGENTE (<= 5 dias)" : "✅ Seguro";
            System.out.printf("#%-4d | %-28s | %3d comp.    | A cada %2dh    | %2d dias rest.   | %s\n",
                    m.getId(), m.getNome(), m.getQuantidadeEstoque(), m.getFrequenciaHoras(), dias, status);
        }
    }

    public void exibirAgendaDoses(List<Dose> doses) {
        System.out.println("\n--- AGENDA DE LEMBRETES E DOSES PENDENTES ---");
        if (doses == null || doses.isEmpty()) {
            System.out.println("Nenhuma dose pendente de confirmação no momento.");
            return;
        }

        for (Dose d : doses) {
            System.out.println(d.toString());
        }
    }

    public void exibirAlertaCritico(Medicamento m) {
        System.out.println("\n" + "!".repeat(75));
        System.out.println("🚨 [ALERTA PROATIVO DE RECOMPRA DISPARADO - REGRA RN03 / RF05]");
        System.out.printf("   O medicamento '%s' possui apenas %d comprimidos em estoque!\n",
                m.getNome(), m.getQuantidadeEstoque());
        System.out.printf("   Autonomia estimada: %d dia(s) de tratamento (menor ou igual a 5 dias).\n",
                m.calcularDiasRestantes());
        System.out.println("   Aviso emitido ao Paciente e Cuidador para providenciar a reposição.");
        System.out.println("!".repeat(75));
    }

    public void exibirPainelCuidador(Responsavel responsavel, Paciente paciente, List<Medicamento> medicamentos) {
        System.out.println("\n" + "=".repeat(75));
        System.out.printf("👤 PAINEL DE GESTÃO FAMILIAR COMPARTILHADA (RF06 / RN08)\n");
        System.out.printf("   Responsável: %s | Vínculo: %s\n", responsavel.getNome(), responsavel.getVinculoFamiliar());
        System.out.printf("   Paciente Sob Cuidado: %s | Diagnóstico: %s\n", paciente.getNome(), paciente.getCondicaoMedica());
        System.out.println("=".repeat(75));

        System.out.println("Situação atual do estoque do paciente:");
        exibirListaMedicamentos(medicamentos);
    }

    public void exibirMensagemSucesso(String msg) {
        System.out.println("✔ [SUCESSO] " + msg);
    }

    public void exibirMensagemErro(String msg) {
        System.err.println("✖ [ERRO] " + msg);
    }

    public String lerTexto(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public int lerInteiro(String prompt) {
        System.out.print(prompt);
        return lerInteiro();
    }

    private int lerInteiro() {
        while (true) {
            try {
                String linha = scanner.nextLine().trim();
                return Integer.parseInt(linha);
            } catch (NumberFormatException e) {
                System.out.print("Entrada inválida. Digite um número inteiro: ");
            }
        }
    }

    public void aguardarEnter() {
        System.out.println("\nPressione [ENTER] para continuar...");
        scanner.nextLine();
    }
}
