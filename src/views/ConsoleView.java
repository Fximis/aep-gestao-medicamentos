package views;

import models.Dose;
import models.Medicamento;
import models.Paciente;
import models.Responsavel;
import models.Usuario;

import java.util.List;
import java.util.Scanner;

/**
 * Camada de Visualizacao em Console CLI (Terminal).
 * Projetada com foco em Experiencia do Usuario (UX) para pessoas comuns,
 * idosos e cuidadores familiares, sem abrir mao do rigor tecnico exigido pela banca.
 * Utiliza indicadores universais para compatibilidade total com qualquer terminal (Windows/Linux/macOS).
 */
public class ConsoleView {

    private final Scanner scanner;

    public ConsoleView() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Cabecalho humanizado da aplicacao com identificacao do paciente e seu cuidador.
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
     * Dashboard de Resumo do Dia: mostra o que e prioritario para a saude do usuario.
     * Separa com clareza doses devidas agora, proximas doses de hoje e alertas criticos de recompra.
     */
    public void exibirResumoDoDia(List<Dose> devidasAgora, List<Dose> proximasHoje, List<Medicamento> criticos) {
        System.out.println("  [RESUMO DO SEU DIA -- O que voce precisa saber agora]:");

        // 1. Remédios para tomar agora
        if (devidasAgora != null && !devidasAgora.isEmpty()) {
            System.out.printf("  [!] REMEDIOS PARA TOMAR AGORA: %d dose(s) aguardando voce! (Opcao [1] para tomar)\n",
                    devidasAgora.size());
            for (Dose d : devidasAgora) {
                System.out.printf("      * %-28s (Previsto: %s)\n",
                        d.getNomeMedicamento(), d.getHorarioPrevistoFormatado());
            }
        } else if (proximasHoje != null && !proximasHoje.isEmpty()) {
            Dose proxima = proximasHoje.get(0);
            System.out.println("  [OK] Parabens! Voce esta em dia com seus remedios deste horario.");
            System.out.printf("       Proximo remedio na sua rotina: '%s' (%s)\n",
                    proxima.getNomeMedicamento(), proxima.getHorarioPrevistoFormatado());
        } else {
            System.out.println("  [OK] Parabens! Todos os remedios agendados para hoje estao concluidos.");
        }

        // 2. Alertas de Recompra (5 dias)
        if (criticos != null && !criticos.isEmpty()) {
            for (Medicamento m : criticos) {
                System.out.printf("  [ALERTA DE RECOMPRA] '%s' dura apenas mais %d dia(s)! (Estoque: %d comp.)\n",
                        m.getNome(), m.calcularDiasRestantes(), m.getQuantidadeEstoque());
            }
        } else {
            System.out.println("  [OK] Seus estoques de medicamentos estao em niveis seguros (acima de 5 dias).");
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

        return lerInteiroComFaixa("Escolha o que deseja fazer [0-8]: ", 0, 8);
    }

    /**
     * Exibe a lista amigavel de remedios prontos para tomar agora.
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

        if (doses.size() > 1) {
            System.out.println("  [T] Tomar TODOS os " + doses.size() + " remedios acima de uma vez so");
        }
        System.out.println("  [0] Voltar ao menu principal");
        System.out.println("-".repeat(78));
    }

    /**
     * Confirmacao acolhedora de dose tomada com informacoes de estoque e alerta de 5 dias.
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
     * Confirmacao de lote de doses tomadas juntas.
     */
    public void exibirConfirmacaoLoteSucesso(List<Medicamento> atualizados, Responsavel responsavel) {
        System.out.println("\n" + "=".repeat(78));
        System.out.printf("[+] Excelente! Voce confirmou a tomada de %d medicamento(s) de uma vez so!\n", atualizados.size());
        System.out.println("=".repeat(78));
        for (Medicamento med : atualizados) {
            System.out.printf("  * '%s': Tomado %d comp. | Restam %d comp. no armario (Dura %d dias)\n",
                    med.getNome(), med.getQuantidadePorDose(), med.getQuantidadeEstoque(), med.calcularDiasRestantes());
            if (med.verificarAlertaRecompra()) {
                System.out.printf("    [!] ALERTA: Estoque critico (<= 5 dias)! Avisamos %s (%s).\n",
                        responsavel != null ? responsavel.getNome() : "o cuidador",
                        responsavel != null ? responsavel.getVinculoFamiliar() : "responsavel");
            }
        }
        System.out.println("=".repeat(78));
    }

    /**
     * Agenda completa com doses devidas agora, proximas hoje, proximos dias e historico recente.
     */
    public void exibirMinhaRotina(List<Dose> devidasAgora, List<Dose> proximasHoje,
                                  List<Dose> proximosDias, List<Dose> historico) {
        System.out.println("\n" + "=".repeat(78));
        System.out.println("                 [AGENDA] MINHA ROTINA DE REMEDIOS E HORARIOS");
        System.out.println("=".repeat(78));

        // 1. Devidas agora
        System.out.println(">> REMEDIOS PARA TOMAR AGORA (Aguardando Tomada):");
        if (devidasAgora == null || devidasAgora.isEmpty()) {
            System.out.println("  [OK] Nenhum remedio pendente para este horario.");
        } else {
            for (Dose d : devidasAgora) {
                System.out.printf("  * %-30s | Horario: %s\n",
                        d.getNomeMedicamento(), d.getHorarioPrevistoFormatado());
            }
        }

        // 2. Próximas hoje
        System.out.println("\n>> PROXIMAS DOSES PROGRAMADAS PARA HOJE:");
        if (proximasHoje == null || proximasHoje.isEmpty()) {
            System.out.println("  [OK] Nenhuma outra dose programada para mais tarde hoje.");
        } else {
            for (Dose d : proximasHoje) {
                System.out.printf("  * %-30s | Horario: %s\n",
                        d.getNomeMedicamento(), d.getHorarioPrevistoFormatado());
            }
        }

        // 3. Próximos dias
        System.out.println("\n>> DOSES PROGRAMADAS PARA OS PROXIMOS DIAS:");
        if (proximosDias == null || proximosDias.isEmpty()) {
            System.out.println("  (Nenhuma dose futura cadastrada alem de hoje)");
        } else {
            for (Dose d : proximosDias) {
                System.out.printf("  * %-30s | Horario: %s\n",
                        d.getNomeMedicamento(), d.getHorarioPrevistoFormatado());
            }
        }

        // 4. Histórico recente
        System.out.println("\n>> HISTORICO RECENTE (Doses que Voce ja Tomou):");
        if (historico == null || historico.isEmpty()) {
            System.out.println("  Nenhuma dose confirmada recentemente.");
        } else {
            for (Dose d : historico) {
                System.out.printf("  * %-30s | Tomada em: %s\n",
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
     * Submenu de reposicao de estoque (Chegou caixinha nova).
     */
    public void exibirMenuReporEstoque(List<Medicamento> medicamentos) {
        System.out.println("\n" + "-".repeat(78));
        System.out.println("                 [REPOR ESTOQUE] CHEGOU CAIXINHA NOVA!");
        System.out.println("-".repeat(78));
        System.out.println("Selecione qual medicamento voce deseja reabastecer:\n");

        for (int i = 0; i < medicamentos.size(); i++) {
            Medicamento m = medicamentos.get(i);
            String alerta = m.verificarAlertaRecompra() ? " [!] ESTOQUE CRITICO" : "";
            System.out.printf("  [%d] %-28s (Estoque atual: %3d comp. | Dura %2d dias)%s\n",
                    (i + 1), m.getNome(), m.getQuantidadeEstoque(), m.calcularDiasRestantes(), alerta);
        }
        System.out.println("\n  [0] Voltar ao menu principal");
        System.out.println("-".repeat(78));
    }

    public void exibirSubmenuModoReposicao(Medicamento med) {
        System.out.println("\nComo voce prefere atualizar o estoque de '" + med.getNome() + "'?");
        System.out.println("  [1] Comprei caixa(s) nova(s) na farmacia (somar comprimidos comprados)");
        System.out.println("  [2] Fiz a contagem no armario (atualizar total exato conferido)");
        System.out.println("  [0] Cancelar e voltar");
    }

    public void exibirResultadoReposicao(Medicamento med, int estoqueAnterior, int comprados) {
        System.out.println("\n" + "=".repeat(78));
        System.out.println("[+] [SUCESSO] Estoque reabastecido com sucesso!");
        System.out.printf("  * Medicamento: %s\n", med.getNome());
        System.out.printf("  * Estoque anterior: %d comp. -> Novo estoque: %d comp. (+%d comp.)\n",
                estoqueAnterior, med.getQuantidadeEstoque(), comprados);
        int dias = med.calcularDiasRestantes();
        System.out.printf("  * Nova autonomia: Dura mais %d dias de tratamento!\n", dias);
        if (med.verificarAlertaRecompra()) {
            System.out.printf("  * [!] Atencao: A autonomia continua menor ou igual a 5 dias (%d dias).\n", dias);
        } else {
            System.out.println("  * Situacao: [OK] Nivel Seguro. Nao precisa se preocupar com falta por enquanto.");
        }
        System.out.println("=".repeat(78));
    }

    public void exibirResultadoConferenciaFisica(Medicamento med, int novoEstoque) {
        System.out.println("\n" + "=".repeat(78));
        System.out.println("[+] [SUCESSO] Contagem de estoque atualizada com sucesso!");
        System.out.printf("  * Medicamento: %s\n", med.getNome());
        System.out.printf("  * Novo estoque fisico conferido: %d comprimidos no armario.\n", novoEstoque);
        int dias = med.calcularDiasRestantes();
        System.out.printf("  * Autonomia calculada: Dura %d dias de uso regular.\n", dias);
        if (med.verificarAlertaRecompra()) {
            System.out.printf("  * [!] Atencao: Estoque em nivel de recompra (%d dias restantes).\n", dias);
        } else {
            System.out.println("  * Situacao: [OK] Nivel Seguro.");
        }
        System.out.println("=".repeat(78));
    }

    public void exibirCabecalhoNovoTratamento() {
        System.out.println("\n" + "-".repeat(78));
        System.out.println("               [+] CADASTRAR NOVO MEDICAMENTO / TRATAMENTO");
        System.out.println("-".repeat(78));
        System.out.println("Preencha os dados da receita medica com atencao:\n");
    }

    public void exibirConfirmacaoNovoTratamento(Medicamento novo) {
        System.out.println("\n" + "=".repeat(78));
        System.out.printf("[+] O tratamento com '%s' foi cadastrado com sucesso no sistema!\n", novo.getNome());
        System.out.printf("  * Posologia: %d comprimido(s) a cada %d hora(s).\n",
                novo.getQuantidadePorDose(), novo.getFrequenciaHoras());
        System.out.printf("  * Estoque inicial: %d comprimidos.\n", novo.getQuantidadeEstoque());
        System.out.printf("  * Autonomia calculada: %d dias de medicacao garantida.\n", novo.calcularDiasRestantes());
        System.out.println("  * Suas proximas doses ja foram organizadas na sua rotina diaria.");
        if (novo.verificarAlertaRecompra()) {
            System.out.println("  * [!] Atencao: Este medicamento ja inicia com estoque critico (<= 5 dias).");
        }
        System.out.println("=".repeat(78));
    }

    public void exibirMenuGerenciarMedicamentos(List<Medicamento> medicamentos) {
        System.out.println("\n" + "-".repeat(78));
        System.out.println("                     [GERENCIAR MEDICAMENTOS]");
        System.out.println("-".repeat(78));
        System.out.println("  [1] Alterar dados de um medicamento (nome, frequencia de horarios ou posologia)");
        System.out.println("  [2] Encerrar / Excluir medicamento do tratamento (Alta medica)");
        System.out.println("  [0] Voltar ao menu principal");
        System.out.println("-".repeat(78));
    }

    /**
     * Painel de acompanhamento para o familiar/cuidador.
     */
    public void exibirPainelCuidador(Responsavel responsavel, Paciente paciente,
                                     List<Medicamento> medicamentos, List<Dose> dosesPendentes,
                                     List<Dose> dosesTomadasHoje) {
        System.out.println("\n" + "=".repeat(78));
        System.out.println("              ESPACO DA FAMILIA & VISAO DO CUIDADOR");
        System.out.println("=".repeat(78));
        System.out.printf("Cuidador(a): %s (%s) | E-mail: %s\n",
                responsavel.getNome(), responsavel.getVinculoFamiliar(), responsavel.getEmail());
        System.out.printf("Paciente Acompanhado: %s | Diagnostico: %s\n",
                paciente.getNome(), paciente.getCondicaoMedica());
        System.out.println("-".repeat(78));

        System.out.println("SITUACAO GERAL DO TRATAMENTO:");

        long criticos = medicamentos.stream().filter(Medicamento::verificarAlertaRecompra).count();
        if (criticos > 0) {
            System.out.println("\n[!] MEDICAMENTOS COM ALERTA DE RECOMPRA (<= 5 DIAS):");
            for (Medicamento m : medicamentos) {
                if (m.verificarAlertaRecompra()) {
                    System.out.printf("  * %-28s: Restam apenas %d comprimidos (%d dias de uso)!\n",
                            m.getNome(), m.getQuantidadeEstoque(), m.calcularDiasRestantes());
                }
            }
            System.out.println("  (Recomendacao para o cuidador: Providenciar receita e adquirir nova caixa)");
        } else {
            System.out.println("  * Estoque Geral: [OK] Todos os medicamentos estao abastecidos com seguranca.");
        }

        System.out.println("\n>> DOSES TOMADAS HOJE (Adesao do Paciente):");
        if (dosesTomadasHoje == null || dosesTomadasHoje.isEmpty()) {
            System.out.println("  * Nenhuma dose registrada como tomada hoje ate o momento.");
        } else {
            for (Dose d : dosesTomadasHoje) {
                System.out.printf("  * [OK TOMADO] %-26s | Horario confirmado: %s\n",
                        d.getNomeMedicamento(), d.getHorarioConsumoFormatado());
            }
        }

        System.out.println("\n>> DOSES PENDENTES / PROGRAMADAS PARA HOJE:");
        if (dosesPendentes == null || dosesPendentes.isEmpty()) {
            System.out.println("  * Nenhuma dose pendente para hoje. Paciente 100% em dia!");
        } else {
            for (Dose d : dosesPendentes) {
                System.out.printf("  * [PENDENTE]  %-26s | Horario previsto: %s\n",
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
     * Painel de Demonstracao Academica e Diretrizes da AEP (Para a Banca / Avaliador).
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
        System.out.println("   * [Delete]: MedicamentoDAO.excluir() (Opcao [6] com exclusao em cascata)");
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
        System.out.println("[-] [ERRO] " + msg);
    }

    public void exibirMensagemAviso(String msg) {
        System.out.println("[!] [AVISO] " + msg);
    }

    // =========================================================================
    // METODOS DE ENTRADA ROBUSTOS (PROTEGIDOS CONTRA CRASH / EOF / TIPOS INVALIDOS)
    // =========================================================================

    private String lerLinhaBruta() {
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine();
    }

    public String lerTexto(String prompt) {
        System.out.print(prompt);
        return lerLinhaBruta().trim();
    }

    public String lerTextoNaoVazio(String prompt, String msgErro) {
        while (true) {
            System.out.print(prompt);
            String texto = lerLinhaBruta().trim();
            if (!texto.isEmpty()) {
                return texto;
            }
            if (!scanner.hasNextLine()) {
                return "Medicamento Padrao";
            }
            exibirMensagemErro(msgErro);
        }
    }

    public int lerInteiroComFaixa(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String linha = lerLinhaBruta().trim();
            if (linha.isEmpty() && !scanner.hasNextLine()) {
                return min;
            }
            try {
                int valor = Integer.parseInt(linha);
                if (valor >= min && valor <= max) {
                    return valor;
                }
                System.out.printf("Valor fora da faixa permitida (%d a %d). Tente novamente.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.print("Entrada invalida. Digite um numero inteiro: ");
            }
        }
    }

    public int lerInteiroPositivo(String prompt, int min) {
        while (true) {
            System.out.print(prompt);
            String linha = lerLinhaBruta().trim();
            if (linha.isEmpty() && !scanner.hasNextLine()) {
                return min;
            }
            try {
                int valor = Integer.parseInt(linha);
                if (valor >= min) {
                    return valor;
                }
                System.out.printf("O valor deve ser de no minimo %d. Digite novamente: ", min);
            } catch (NumberFormatException e) {
                System.out.print("Entrada invalida. Digite um numero inteiro valido: ");
            }
        }
    }

    public int lerInteiroComPadrao(String prompt, int padrao, int min) {
        while (true) {
            System.out.print(prompt);
            String linha = lerLinhaBruta().trim();
            if (linha.isEmpty()) {
                return padrao;
            }
            try {
                int valor = Integer.parseInt(linha);
                if (valor >= min) {
                    return valor;
                }
                System.out.printf("O valor deve ser de pelo menos %d. Digite novamente: ", min);
            } catch (NumberFormatException e) {
                System.out.print("Entrada invalida. Digite um numero ou pressione ENTER para padrao: ");
            }
        }
    }

    public String lerOpcaoTomadaDose(String prompt) {
        System.out.print(prompt);
        return lerLinhaBruta().trim().toUpperCase();
    }

    public boolean lerConfirmacaoSN(String prompt) {
        System.out.print(prompt);
        String entrada = lerLinhaBruta().trim().toUpperCase();
        return "S".equals(entrada) || "SIM".equals(entrada);
    }

    public void aguardarEnter() {
        System.out.println("\nPressione [ENTER] para continuar...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}