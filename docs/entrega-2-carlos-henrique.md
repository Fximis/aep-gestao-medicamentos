# Segunda entrega - escopo de Carlos Henrique

## Responsabilidades e integracao

| Responsavel | Escopo | Situacao neste pacote |
| --- | --- | --- |
| Carlos Henrique | DoseDAO, confirmacao e baixa, menu CLI, painel do cuidador | Implementado e validado no projeto recebido |
| Carlos Henrique | Integracao do ponto de entrada Main com o menu | Implementado |
| Lucas Thomaz | Modelos, encapsulamento, RN04 no modelo, autonomia, alerta e GenericDAO | Arquivos preservados; validar novamente apos a entrega dele |
| Equipe | Banco, drivers, demais DAOs e GUI existentes | Reutilizados do ZIP, sem atribuir sua autoria ao Carlos |

Este documento complementa o cronograma original; nao altera datas nem compromissos
da primeira entrega. A divisao acima permite integrar as contribuicoes por arquivo
e conferir os commits de cada integrante.

## Conteudos do semestre aplicados

| Conteudo | Aplicacao nesta contribuicao |
| --- | --- |
| Banco de dados | JDBC, SELECT com JOIN, INSERT, UPDATE, DELETE, chaves existentes e transacoes com commit/rollback |
| Java / POO | ConsoleView e DoseDAO com responsabilidades distintas; dependencia do contrato GenericDAO<Medicamento>; uso das entidades e dos metodos de dominio do Lucas |
| Gestao de projetos tecnologicos | Divisao de responsabilidades, contrato de integracao, criterios de aceite, rastreabilidade, roteiro de testes e controle de contribuicao por commit |

Heranca, polimorfismo da hierarquia de usuarios e implementacao dos modelos continuam
no escopo do Lucas. O pacote usa essas classes; nao pretende substituir a entrega
dele ou garantir sozinho todos os criterios da avaliacao final do grupo.

## Rastreabilidade e criterios de aceite

| Requisito / regra | Implementacao | Evidencia |
| --- | --- | --- |
| RF01 - Cadastro | Opcao 1 e DoseDAO.cadastrarTratamento | Medicamento e quatro doses sao inseridos na mesma transacao |
| RF02 - Lembretes | listarPendentesPorPaciente e resumo ao abrir o menu | Pendencias ordenadas por horario, sem baixar estoque pela passagem do tempo |
| RF03 / RN01 / RN06 - Confirmacao | Opcao 6 e confirmarTomadaDose(id, pacienteId) | Confirma consumo real, grava horario e deduz quantidade por dose |
| RF04 / RN05 - Estoque e autonomia | Baixa condicional em SQL e metodos de Medicamento | Estoque relido do banco e autonomia calculada pelo modelo |
| RF05 / RN03 - Recompra | exibirAlerta e verificarAlertaRecompra | Alerta quando autonomia inteira calculada pelo modelo e <= 5 dias |
| RN04 - Nao negatividade | Modelo existente e condicao no UPDATE de estoque | Falta de estoque provoca rollback sem confirmar a dose |
| RF06 / RN08 - Cuidador | Opcao 7 | O paciente autoriza a conta do responsável a consultar estoque, pendências e histórico |
| RF07 / RN07 - Ajuste | Opcao 4 e MedicamentoDAO.atualizarEstoque | Grava novo total fisico, validado contra negativos |
| CRUD completo | Opcoes 1, 2, 3, 5 | Dados consultados por novas conexoes; exclusao remove doses associadas |

O consentimento é concedido e revogado pela conta do paciente. A consulta é
feita pelo responsável autenticado. [ADR-005](adr/2026-11-11-adr-005-autenticacao-e-controle-de-acesso.md)

## Contratos esperados do trabalho do Lucas

Todos os pacotes e nomes abaixo sao os do ZIP recebido.

- `GenericDAO<T>`: `void inserir(T)`, `T buscarPorId(int)`,
  `List<T> listarTodos()`, `void atualizar(T)`, `void excluir(int)`,
  com `throws SQLException`.
- `Medicamento`: construtores
  `(int usuarioId, String nome, int estoque, int frequenciaHoras, int quantidadePorDose)`
  e `(int id, int usuarioId, String nome, int estoque, int frequenciaHoras, int quantidadePorDose)`.
- `Medicamento`: getters de todos esses atributos, setters de id, nome, estoque,
  frequencia e quantidadePorDose; `int calcularDiasRestantes()` e
  `boolean verificarAlertaRecompra()`.
- `Dose`: construtor
  `(int id, int medicamentoId, String nomeMedicamento, LocalDateTime previsto, LocalDateTime consumo, boolean tomado)`;
  getters, `setId(int)`, `isStatusTomado()`, `isHoje()`, `isDevidaAgora()`.
- `Paciente`: `getId()`, `getNome()`.
- `Responsavel`: `getNome()`, `getVinculoFamiliar()`.
- `Paciente` e `Responsavel` continuam sendo subclasses de `Usuario`.

A DAO nao depende do tipo de retorno de `reduzirEstoque`: a baixa e feita por
UPDATE condicional no banco e o modelo e reconstruido com o estoque confirmado.
Assim, a protecao de RN04 no modelo e a integridade transacional se complementam.

## Fluxo transacional de confirmacao

1. Abre nova conexao e desativa autoCommit.
2. Altera a dose somente se ainda estiver pendente; no console, verifica o paciente.
3. Deduz `quantidade_por_dose` diretamente no banco somente se houver estoque suficiente.
4. Relê o medicamento atualizado.
5. Se houver menos de tres doses pendentes, agenda mais uma sem baixar estoque.
6. Confirma tudo com commit. Qualquer falha provoca rollback e a conexao e fechada.

A baixa usa subtracao no banco, em vez de gravar um saldo lido anteriormente.
Isso evita perda de atualizacao. Confirmar a mesma dose novamente nao baixa o saldo.
A API de confirmacao em lote foi mantida por compatibilidade com a GUI e tambem e atomica.

Cadastro + agenda inicial e alteracao de posologia + nova agenda tambem sao atomicos.
A atualizacao de posologia preserva doses tomadas e o estoque atual; a atualizacao
somente do nome preserva a agenda. Nenhuma classe do Lucas e reimplementada.

## Evidencias da validacao

Ambiente: Windows, JDK 21.0.9, driver SQLite do ZIP original.
Resultado: **42 verificacoes aprovadas**.
Os testes nao usam mocks de persistencia nem uma lista em memoria como banco.

Cobertura: cadastro atomico; agenda; pendencias ordenadas; baixa maior que uma unidade;
autonomia e limite de cinco dias; horario de consumo; duplicidade; estoque insuficiente;
dose inexistente; outro paciente; falha SQL e rollback; alteracao de nome e posologia;
ajuste persistido e negativo; lote atomico; duas conexoes concorrentes; todas as sete
opcoes do console; consentimento; cancelamento; entrada invalida; EOF sem insercao parcial;
exclusao com remocao do historico.

Os testes usam SQLite em diretório temporário. A propriedade `aep.banco` seleciona
o banco e o teste cadastra as contas usadas pelo console. O ADR-005 registra
a validação mais recente do fluxo de conta.

## Console e testes no projeto integrado

A contribuição faz parte da versão integrada. Os pré-requisitos, as opções de banco e o procedimento de cadastro estão no [guia de execução](../README.md).

No PowerShell, a partir da raiz do projeto, use os seguintes comandos para iniciar o console ou executar os testes de integração, respectivamente.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\executar-carlos.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\testar-carlos.ps1
```

Em Linux, abra o console com `bash scripts/executar-carlos.sh`. O teste de integração usa SQLite em um diretório temporário, com dados próprios da execução.

No menu do paciente, as opções 1 a 5 cuidam do cadastro, da consulta, da edição, do ajuste de estoque e da exclusão de medicamentos. A opção 6 confirma a dose, a 7 gerencia o acesso familiar e a 8 altera a senha. A opção 0 encerra a sessão.
