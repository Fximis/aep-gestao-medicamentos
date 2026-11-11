# ADR-004: Encapsulamento dos Modelos, Autonomia Derivada e Evolução da Interface

**Status:** Vigente  
**Data:** 2026-11-10  
**Responsável pela contribuição:** Lucas Thomaz  
**Relacionados:** [ADR-002](2026-11-10-adr-002-padrao-dao-e-interface-generica.md) e [ADR-003](2026-11-10-adr-003-interface-cli-humanizada-e-painel-tecnico.md)

---

## Contexto

A revisão da 2ª Entrega identificou pontos a reforçar nos pilares de POO e nas regras de estoque. A classe abstrata `Usuario` mantinha atributos `protected`, e `Paciente` expunha diretamente sua lista interna de medicamentos. Isso permitia que código externo alterasse a estrutura da coleção sem passar pelos métodos da entidade.

`Medicamento` já bloqueava estoque negativo pelo setter e consumo acima do saldo. Entretanto, a soma de uma reposição podia ultrapassar o limite do tipo `int` e produzir um número negativo. A interface gráfica também calculava a autonomia de reposição por uma fórmula própria com divisão inteira, que podia gerar resultados incorretos ou divisão por zero em intervalos maiores que 24 horas.

A interface `GenericDAO<T>` já existia e era implementada por `MedicamentoDAO`. Portanto, a necessidade era revisar e documentar seu contrato, preservando a compatibilidade com a persistência existente.

Também foi solicitada uma apresentação gráfica mais organizada, mantendo Java Swing e os fluxos de cadastro, edição, reposição e confirmação de doses.

## Decisão

### 1. Reforçar o encapsulamento dos modelos

- Tornar `private` os atributos de instância de `Usuario`, mantendo acesso pelos getters/setters e construção pela hierarquia existente.
- Manter atributos de instância privados nas demais entidades.
- Em `Paciente`, manter uma lista interna privada e `final`, copiar a coleção recebida pelo setter e devolver uma cópia não modificável pelo getter. Rejeitar listas e medicamentos nulos antes de alterar o estado.
- Acrescentar construtores sem ID em `Paciente`, `Responsavel` e `Dose` para representar novos registros antes de o banco gerar o identificador.

**Por quê:** impedir alterações diretas nos atributos e na estrutura da coleção, centralizando os caminhos de acesso e preservando herança e polimorfismo.

### 2. Proteger todos os caminhos de alteração do estoque (RN04)

- Preservar a rejeição de estoque negativo no construtor e em `setQuantidadeEstoque`.
- Manter o retorno `false` de `reduzirEstoque` quando a quantidade for não positiva ou maior que o saldo, sem modificar o objeto.
- Validar quantidades positivas em `adicionarEstoque`, realizar a soma em `long` e rejeitar resultados maiores que `Integer.MAX_VALUE` antes de atribuir ao estoque.
- Utilizar o setter validado para efetivar as mudanças de saldo.

**Por quê:** a RN04 também precisa valer para reposições e valores extremos, e não apenas para a atribuição direta de um número negativo.

### 3. Derivar a autonomia dos dados atuais (RN05)

Calcular os dias completos a cada chamada de `calcularDiasRestantes()`:

```text
dias = (estoque × frequência em horas) / (24 × quantidade por dose)
```

O cálculo usa `long` nos resultados intermediários, descarta a parte fracionária e limita resultados extremos ao maior `int`. Frequência e quantidade por dose permanecem estritamente positivas. Não será mantido um atributo separado de autonomia em cache.

**Por quê:** mudanças de estoque, intervalo ou dose devem aparecer automaticamente na próxima consulta, sem depender de sincronização de um segundo campo. A fórmula evita a divisão inteira prematura de `24 / frequenciaHoras`.

### 4. Consultar a autonomia atual para o alerta (RN03 / RF05)

Manter `verificarAlertaRecompra()` vinculado a `calcularDiasRestantes() <= 5`. As telas consultam esse resultado após as operações e apresentam o destaque de estoque e os avisos ao usuário.

**Por quê:** centralizar o limite de recompra no domínio e manter os modelos independentes de janelas, mensagens e acesso ao banco.

### 5. Formalizar o contrato existente de persistência

Revisar a documentação de `GenericDAO<T>`, preservando as assinaturas de `inserir`, `buscarPorId`, `listarTodos`, `atualizar` e `excluir`, todas com declaração de `SQLException`.

Documentar que a inserção preenche o ID gerado, a busca sem resultado retorna `null` e a listagem sem registros retorna uma lista vazia.

**Por quê:** tornar explícito o comportamento esperado para quem usa ou implementa o contrato, sem quebrar `MedicamentoDAO`. Esta decisão complementa o ADR-002.

### 6. Padronizar a apresentação gráfica

- Criar `views.gui.Tema` para compartilhar cores, tipografia, cartões arredondados, botões, tabelas e barras de rolagem.
- Usar Look and Feel multiplataforma com personalização em Swing, substituindo a escolha do tema nativo por sistema.
- Substituir as abas por navegação lateral com `CardLayout`: rotina, medicamentos, família e apresentação técnica.
- Exibir cartões de resumo para doses pendentes, necessidade de recompra e progresso diário.
- Priorizar doses pendentes na ordenação da rotina e preservar as operações existentes e o modo CLI.
- Na reposição gráfica, usar `Medicamento.adicionarEstoque()` e `calcularDiasRestantes()` em vez de repetir as regras na tela.

**Por quê:** dar consistência visual às telas, facilitar a leitura das informações e eliminar o cálculo de autonomia divergente na interface. Esta decisão atualiza os itens 1 e 2 do ADR-003; a escolha de Swing, os alertas e o modo CLI permanecem vigentes.

## Alternativas consideradas

| Alternativa | Motivo para não adotar |
| --- | --- |
| Guardar dias restantes em um novo atributo atualizável | Exigiria manter o valor sincronizado em cada alteração de estoque, intervalo e dose. |
| Validar estoque apenas na tela ou no DAO | Não protegeria o objeto quando utilizado por outro fluxo ou teste. |
| Retornar diretamente a lista de medicamentos | Permitiria incluir ou remover itens sem passar pelos métodos do modelo. |
| Alterar as assinaturas do contrato DAO | Criaria incompatibilidade desnecessária com a implementação existente. |
| Adicionar uma biblioteca gráfica externa | Swing já atende à proposta e mantém a compilação sem novas dependências. |

## Consequências

**Positivas:**

- Encapsulamento mais rígido e regras de estoque centralizadas no modelo.
- Autonomia sempre baseada nos valores atuais, com alerta coerente após alterações.
- Interface com apresentação uniforme e reaproveitamento dos componentes visuais.
- Contrato de persistência explícito e compatível com a implementação existente.
- Rastreabilidade da contribuição por arquivos, testes e decisões documentadas.

**Limitações e cuidados:**

- Código que tentasse alterar diretamente a coleção retornada por `getMedicamentos()` deve usar `adicionarMedicamento` ou `setMedicamentos`.
- A cópia protege a estrutura da lista; cada `Medicamento` continua sendo uma entidade mutável por seus métodos públicos.
- A assinatura de autonomia continua sendo `int`, com dias completos, por compatibilidade com as telas.
- Reposições inválidas passam a lançar `IllegalArgumentException`; a GUI de reposição trata essa validação.

## Evidências de validação

- Compilação completa com Java 21.
- 72 verificações aprovadas em `tests/ModelosTest.java`, incluindo encapsulamento, RN04, autonomia, limites de 5 e 6 dias e contrato DAO.
- Testes de integração em SQLite temporário para CRUD, confirmação de doses, alerta, rejeição de dose duplicada e rollback por estoque insuficiente.
- Revisão visual das quatro páginas, tamanho mínimo de janela e formulários de cadastro e reposição.

A integração com um servidor MySQL ativo não foi executada nesta revisão.

O mapa de arquivos e as instruções para reproduzir os testes estão em [Modelos, contrato DAO e interface gráfica](../2a_entrega_modelos_interface.md).
