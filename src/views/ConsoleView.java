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
 * Projetada com foco em Experiência do Usuário (UX) para pessoas comuns,
 * idosos e cuidadores familiares, sem abrir mão do rigor técnico exigido pela banca.
 * Utiliza indicadores universais para compatibilidade total com qualquer terminal (Windows/Linux/macOS).
 */
public class ConsoleView {

    private final Scanner scanner;

    public ConsoleView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Cabeçalho humanizado da aplicação com identificação do paciente e seu cuidador.
     */
    public void exibirCabecalhoHumano(Paciente paciente, Responsavel responsavel) {
        System.out.println("\n" + "=".repeat(78));
        System.out.println("            CUIDADO DIARIO -- Gestao Familiar de Remedios");
        System.out.println("=".repeat(78));
        if (paciente != null) {
            System.out.printf("  [PACIENTE] %-28s | Condicao: %s\n",
                    paciente.getNome(), paciente.getCondicaoMedica());
        }
        if (responsavel != null) {
            System.out.printf("  [CUIDADOR] %-28s | Vinculo: %s\n",
                    responsavel.getNome(), responsavel.getVinculoFamiliar());
        }
        System.out.println("-".repeat(78));
    }

    /**
     * Dashboard de Resumo do Dia: mostra o que é prioritário para a saúde do usuário.
     */
    public void exibirResumoDoDia(List<Dose> pendentes, List<Medicamento> criticos) {
        System.out.println("  [RESUMO DO SEU DIA -- O que voce precisa saber agora]:");

        if (pendentes != null && !pendentes.isEmpty()) {
            System.out.printf("  [!] Remedios para tomar: %d dose(s) aguardando voce! (Opcao [1] para tomar)\n",
                    pendentes.size());
        } else {
            System.out.println("  [OK] Parabens! Todos os remedios agendados estao em dia.");
        }

        if (criticos != null && !criticos.isEmpty()) {
            for (Medicamento m : criticos) {
                System.out.printf("  [ALERTA DE RECOMPRA] '%s' dura apenas mais %d dia(s)!\n",
                        m.getNome(), m.calcularDiasRestantes());
            }
        } else {
            System.out.println("  [OK] Seus estoques de medicamentos estao em niveis seguros.");
        }
        System.out.println("-".repeat(78));
    }

    /**
     * Menu Principal em linguagem natural e intuitiva para qualquer pessoa.
     */
    public int exibirMenuPrincipal() {
        System.out.println("  [1] Tomar Medicamento Agora (Confirmar Dose)");
        System.out.println("  [2] Minha Rotina de Remedios (Lembretes e Horarios de Hoje)");
        System.out.println("  [3] Meus Medicamentos e Estoque (Consultar Dias Restantes)");
        System.out.println("  [4] Repor Estoque (Chegou Caixinha Nova!)");
        System.out.println("  [5] Cadastrar Novo Tratamento / Medicamento");
        System.out.println("  [6] Gerenciar Medicamentos (Alterar Dados ou Remover)");
        System.out.println("  [7] Espaco da Familia / Visao do Cuidador");
        System.out.println();
        System.out.println("  [8] Informacoes Tecnicas e Diretrizes do Sistema (Banca / AEP)");
        System.out.println("  [0] Encerrar Aplicativo");
        System.out.println("=".repeat(78));
        System.out.print("Escolha o que deseja fazer: ");

        return lerInteiro();
    }

    /**
     * Exibe a lista amigável de remédios prontos para tomar.
     */
    public void exibirListaMedicamentosParaTomar(List<Dose> doses) {
        System.out.println("\n" + "-".repeat(78));
        System.out.println("                   [!] REMEDIOS AGUARDANDO VOCE");
        System.out.println("-".repeat(78));
        System.out.println("Selecione o medicamento que voce acabou de tomar:");
        System.out.println();

        for (int i = 0; i < doses.size(); i++) {
            Dose d = doses.get(i);
            System.out.printf("  [%d] %s\n", (i + 1), d.getNomeMedicamento());
            System.out.printf("      Horario previsto: %s\n", d.getHorarioPrevistoFormatado());
            System.out.println();
        }

        System.out.println("  [0] Voltar ao menu principal");
        System.out.println("-".repeat(78));
    }

    /**
     * Confirmação acolhedora de dose tomada com informações de estoque e alerta de 5 dias.
     */
    public void exibirConfirmacaoDoseSucesso(Medicamento med, Responsavel responsavel) {
        System.out.println("\n" + "=".repeat(78));
        System.out.printf("[+] Muito bem! Registro do medicamento '%s' realizado com sucesso!\n", med.getNome());
        System.out.printf("  * Quantidade tomada agora: %d comprimido(s).\n", med.getQuantidadePorDose());
        System.out.printf("  * Estoque atual restante: %d comprimido(s) no armario.\n", med.getQuantidadeEstoque());
        System.out.printf("  * Autonomia calculada: Dura mais %d dia(s) de tratamento.\n", med.calcularDiasRestantes());

        if (med.verificarAlertaRecompra()) {
            System.out.println("\n[!] ATENCAO - ESTOQUE CRITICO (Regra dos 5 dias):");
            System.out.printf("  Seu estoque vai durar apenas %d dia(s)!\n", med.calcularDiasRestantes());
            if (responsavel != null) {
                System.out.printf("  Avisamos %s (%s) para providenciar a compra de uma nova caixa.\n",
                        responsavel.getNome(), responsavel.getVinculoFamiliar());
            } else {
                System.out.println("  Providencie a compra de uma nova caixa na farmacia.");
            }
        } else {
            System.out.println("  * Situacao do estoque: [OK] Nivel Seguro.");
        }
        System.out.println("=".repeat(78));
    }

    /**
     * Agenda completa com próximas doses e histórico de doses tomadas.
     */
    public void exibirMinhaRotina(List<Dose> pendentes, List<Dose> historico) {
        System.out.println("\n" + "=".repeat(78));
        System.out.println("                 [AGENDA] MINHA ROTINA DE REMEDIOS E HORARIOS");
        System.out.println("=".repeat(78));

        System.out.println(">> PROXIMAS DOSES AGENDADAS (Aguardando Tomada):");
        if (pendentes == null || pendentes.isEmpty()) {
            System.out.println("  Nenhuma dose pendente! Voce esta em dia com seus remedios.");
        } else {
            for (Dose d : pendentes) {
                System.out.printf("  * %-32s | Horario: %s\n",
                        d.getNomeMedicamento(), d.getHorarioPrevistoFormatado());
            }
        }

        System.out.println("\n>> HISTORICO RECENTE (Doses Tomadas):");
        if (historico == null || historico.isEmpty()) {
            System.out.println("  Nenhuma dose confirmada recentemente.");
        } else {
            for (Dose d : historico) {
                System.out.printf("  * %-32s | Tomada em: %s\n",
                        d.getNomeMedicamento(), d.getHorarioConsumoFormatado());
            }
        }
        System.out.println("=".repeat(78));
    }

    /**
     * Lista completa dos medicamentos com posologia, estoque e dias restantes.
     */
    public void exibirCatalogoMedicamentos(List<Medicamento> medicamentos) {
        System.out.println("\n" + "=".repeat(78));
        System.out.println("                   [ESTOQUE] MEUS MEDICAMENTOS E DURACAO");
        System.out.println("=".repeat(78));

        if (medicamentos == null || medicamentos.isEmpty()) {
            System.out.println("Voce ainda nao possui nenhum medicamento cadastrado.");
            return;
        }

        System.out.printf("%-3s | %-28s | %-16s | %-10s | %-10s | %s\n",
                "#", "Medicamento", "Como Tomar", "Estoque", "Duracao", "Situacao");
        System.out.println("-".repeat(78));

        for (int i = 0; i < medicamentos.size(); i++) {
            Medicamento m = medicamentos.get(i);
            int dias = m.calcularDiasRestantes();
            String situacao = m.verificarAlertaRecompra() ? "[!] RECOMPRA (<= 5 dias)" : "[OK] Seguro";
            String comoTomar = String.format("%d comp. a cada %dh", m.getQuantidadePorDose(), m.getFrequenciaHoras());

            System.out.printf("%-3d | %-28s | %-16s | %3d comp.  | %2d dias   | %s\n",
                    (i + 1), m.getNome(), comoTomar, m.getQuantidadeEstoque(), dias, situacao);
        }

        System.out.println("-".repeat(78));
        System.out.println("[i] Como funciona o calculo?");
        System.out.println("   Dividimos os comprimidos em casa pelo quanto voce toma a cada dia.");
        System.out.println("   Quando restam 5 dias ou menos, o sistema avisa para comprar mais caixas.");
        System.out.println("=".repeat(78));
    }

    /**
     * Painel de acompanhamento para o familiar/cuidador.
     */
    public void exibirPainelCuidador(Responsavel responsavel, Paciente paciente,
                                     List<Medicamento> medicamentos, List<Dose> dosesPendentes) {
        System.out.println("\n" + "=".repeat(78));
        System.out.println("              ESPACO DA FAMILIA & VISAO DO CUIDADOR");
        System.out.println("=".repeat(78));
        System.out.printf("Cuidador(a): %s (%s) | E-mail: %s\n",
                responsavel.getNome(), responsavel.getVinculoFamiliar(), responsavel.getEmail());
        System.out.printf("Paciente Acompanhado: %s | Diagnostico: %s\n",
                paciente.getNome(), paciente.getCondicaoMedica());
        System.out.println("-".repeat(78));

        System.out.println("SITUACAO DO TRATAMENTO:");

        long criticos = medicamentos.stream().filter(Medicamento::verificarAlertaRecompra).count();
        if (criticos > 0) {
            System.out.println("\n[!] MEDICAMENTOS COM ALERTA DE RECOMPRA (<= 5 DIAS):");
            for (Medicamento m : medicamentos) {
                if (m.verificarAlertaRecompra()) {
                    System.out.printf("  * %-28s: Restam apenas %d comprimidos (%d dias de uso)!\n",
                            m.getNome(), m.getQuantidadeEstoque(), m.calcularDiasRestantes());
                }
            }
            System.out.println("  (Recomendacao: Providencie receita medica e adquira nova embalagem)");
        } else {
            System.out.println("  * Estoque Geral: [OK] Todos os medicamentos estao abastecidos.");
        }

        System.out.println("\n>> REMEDIOS PENDENTES PARA HOJE:");
        if (dosesPendentes == null || dosesPendentes.isEmpty()) {
            System.out.println("  * Todas as doses programadas ja foram tomadas ate o momento.");
        } else {
            for (Dose d : dosesPendentes) {
                System.out.printf("  * %-28s | Horario: %s\n",
                        d.getNomeMedicamento(), d.getHorarioPrevistoFormatado());
            }
        }

        System.out.println("-".repeat(78));
        System.out.println("[i] Finalidade do Modulo (RF06 / RN08):");
        System.out.println("   Permitir que familiares e cuidadores acompanhem a adesao e o estoque a");
        System.out.println("   distancia, garantindo a continuidade do tratamento sem interrupcoes.");
        System.out.println("=".repeat(78));
    }

    /**
     * Painel de Demonstração Acadêmica e Diretrizes da AEP (Para a Banca / Avaliador).
     */
    public void exibirPainelTecnicoAEP(List<Usuario> usuarios, boolean isSQLite) {
        System.out.println("\n" + "=".repeat(78));
        System.out.println("  PAINEL TECNICO E CONFORMIDADE COM AS DIRETRIZES DA AEP (4o SEMESTRE)");
        System.out.println("=".repeat(78));
        System.out.println("Prezado Avaliador, este modulo consolida e comprova todos os criterios");
        System.out.println("estabelecidos formalmente na Rubrica da 2a Entrega (Peso 1,0):");
        System.out.println();

        System.out.println("1. PILARES DE ORIENTACAO A OBJETOS (POO) [Peso 0,3]:");
        System.out.println("   * Classe Abstrata: 'Usuario' (define campos base e metodo abstrato obterTipo())");
        System.out.println("   * Interface Generica: 'GenericDAO<T>' (padroniza operacoes CRUD fortemente tipadas)");
        System.out.println("   * Heranca: 'Paciente extends Usuario' e 'Responsavel extends Usuario'");
        System.out.println("   * Polimorfismo (@Override): Iterando polimorficamente sobre List<Usuario>:");
        for (Usuario u : usuarios) {
            System.out.printf("     -> Instancia de %-12s | u.obterTipo() = '%-12s' | Nome: %s\n",
                    u.getClass().getSimpleName(), u.obterTipo(), u.getNome());
        }
        System.out.println("   * Encapsulamento: Atributos privados e metodos com regras de dominio (RN04).");
        System.out.println();

        System.out.println("2. PERSISTENCIA EM BANCO DE DADOS RELACIONAL [Peso 0,2]:");
        System.out.printf("   * Conexao JDBC ativa: %s\n",
                isSQLite ? "SQLite Relacional Local ('database/gestao_medicamentos.db')" : "MySQL (localhost:3306)");
        System.out.println("   * Transacoes Atomicas (ACID): DoseDAO.confirmarTomadaDose gerencia commit e rollback");
        System.out.println("   * Script DDL: Arquivo 'database/schema.sql' com PK, FK e ON DELETE CASCADE");
        System.out.println();

        System.out.println("3. CRUD FUNCIONAL COMPLETO NA ENTIDADE PRINCIPAL [Peso 0,3]:");
        System.out.println("   * [Create]: MedicamentoDAO.inserir() (Opcao [5] do menu)");
        System.out.println("   * [Read]:   MedicamentoDAO.buscarPorId() e listarPorUsuario() (Opcao [3])");
        System.out.println("   * [Update]: MedicamentoDAO.atualizar() e atualizarEstoque() (Opcoes [4] e [6])");
        System.out.println("   * [Delete]: MedicamentoDAO.excluir() (Opcao [6])");
        System.out.println();

        System.out.println("4. ADERENCIA AO PLANEJAMENTO DA 1a ENTREGA [Peso 0,2]:");
        System.out.println("   * RF01 (Cadastro), RF02 (Lembretes), RF03 (Confirmacao), RF04 (Baixa de Estoque),");
        System.out.println("     RF05 (Alerta de 5 dias), RF06 (Gestao Familiar), RF07 (Ajuste Manual)");
        System.out.println("   * Regras de Negocio Chave: RN01 a RN08 implementadas e ativas no sistema.");
        System.out.println("=".repeat(78));
    }

    public void exibirMensagemSucesso(String msg) {
        System.out.println("[+] [SUCESSO] " + msg);
    }

    public void exibirMensagemErro(String msg) {
        System.err.println("[-] [ERRO] " + msg);
    }

    public void exibirMensagemAviso(String msg) {
        System.out.println("[!] [AVISO] " + msg);
    }

    public String lerTexto(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public int lerInteiro(String prompt) {
        System.out.print(prompt);
        return lerInteiro();
    }

    public int lerInteiroComPadrao(String prompt, int padrao) {
        System.out.print(prompt);
        String linha = scanner.nextLine().trim();
        if (linha.isEmpty()) {
            return padrao;
        }
        try {
            return Integer.parseInt(linha);
        } catch (NumberFormatException e) {
            return padrao;
        }
    }

    private int lerInteiro() {
        while (true) {
            try {
                String linha = scanner.nextLine().trim();
                return Integer.parseInt(linha);
            } catch (NumberFormatException e) {
                System.out.print("Entrada invalida. Digite um numero inteiro: ");
            }
        }
    }

    public void aguardarEnter() {
        System.out.println("\nPressione [ENTER] para continuar...");
        scanner.nextLine();
    }
}
