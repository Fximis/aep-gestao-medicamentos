# 2ª entrega — Modelos, contrato DAO e interface gráfica

Este registro descreve a revisão anterior dos modelos e da interface. Para o fluxo de acesso e os testes atuais, consulte o [ADR-005](adr/2026-11-11-adr-005-autenticacao-e-controle-de-acesso.md).

## O que foi entregue

| Arquivo | Responsabilidade e alterações |
| --- | --- |
| `src/models/Usuario.java` | Classe abstrata com todos os atributos de instância `private`, construtor e getters/setters. |
| `src/models/Paciente.java` | Herança e polimorfismo preservados. Lista interna privada; cópia defensiva no setter e cópia não modificável no getter. Construtores com e sem ID. |
| `src/models/Responsavel.java` | Atributo privado, getter/setter e construtores com e sem ID. |
| `src/models/Dose.java` | Atributos privados, getters/setters e construtores para agendamento e leitura do histórico. Novo construtor sem ID para cadastro. |
| `src/models/Medicamento.java` | Validação do estoque, reposição protegida contra overflow, cálculo derivado de autonomia e alerta de recompra. |
| `src/dao/GenericDAO.java` | Contrato genérico revisado e documentado com cinco operações padronizadas; assinaturas existentes preservadas para compatibilidade. |
| `src/views/gui/Tema.java` | Cores, cartões arredondados, botões com estados de foco e mouse, tabelas e barras de rolagem compartilhados em Swing. |
| `src/views/gui/MainWindow.java` | Menu lateral, resumo de doses/estoque/progresso, páginas de rotina, medicamentos, família e apresentação técnica. |
| `src/views/gui/MedicamentoDialog.java` e `ReporEstoqueDialog.java` | Formulários alinhados ao novo tema. |
| `src/Main.java` | Inicialização do tema antes de abrir a janela. |
| `tests/ModelosTest.java` | 72 verificações de domínio e do contrato da interface, sem dependências de teste externas. |

A interface `GenericDAO` e parte das regras já existiam no projeto recebido. O trabalho manteve o contrato e reforçou as validações e o encapsulamento.

## RN04: estoque nunca negativo

A classe `Medicamento` protege todos os seus caminhos de alteração de estoque:

- Construtor e `setQuantidadeEstoque`: lançam `IllegalArgumentException` se o estoque for negativo.
- `reduzirEstoque`: retorna `false` para quantidade não positiva ou estoque insuficiente, sem alterar o saldo. Retorna `true` para consumo permitido, inclusive se o saldo chegar a zero.
- `adicionarEstoque`: exige quantidade positiva e verifica o resultado com `long` antes de atribuir ao `int`. Isso impede que uma soma acima de `Integer.MAX_VALUE` vire um valor negativo. Entradas inválidas geram `IllegalArgumentException` e preservam o saldo.
- Frequência em horas e quantidade por dose devem ser maiores que zero.

O construtor vazio mantém frequência e dose positivas para permitir o preenchimento pelos setters. O formulário fornece os valores do cadastro.

## RN05: recálculo de autonomia

```text
dias completos = (estoque × frequência em horas) / (24 × quantidade por dose)
```

`calcularDiasRestantes()` calcula o resultado a cada consulta a partir dos atributos atuais. Não há um campo duplicado de dias que possa ficar desatualizado: mudanças de estoque, frequência ou dose são refletidas automaticamente na próxima consulta.

O cálculo usa `long`, descarta a fração de dia e limita resultados extremos a `Integer.MAX_VALUE`, mantendo a assinatura `int` usada pelas telas existentes. Intervalos como 5 horas ou 48 horas funcionam sem a divisão inteira prematura de `24 / frequenciaHoras`.

Exemplo com 1 unidade a cada 8 horas:

| Estoque | Dias completos | Alerta |
| --- | --- | --- |
| 60 | 20 | Não |
| 18 | 6 | Não |
| 15 | 5 | Sim |
| 0 | 0 | Sim |

## RN03: alerta proativo

`verificarAlertaRecompra()` consulta a autonomia atual e retorna `true` quando `calcularDiasRestantes() <= 5`. As telas usam o retorno para destacar medicamentos e atualizar os avisos após confirmação, edição e reposição. Os modelos não abrem janelas nem dependem da interface gráfica.

Na reposição pela GUI, o novo estoque agora passa por `Medicamento.adicionarEstoque()`, e a mensagem usa `calcularDiasRestantes()`. Isso elimina um cálculo separado que poderia apresentar autonomia errada ou dividir por zero em intervalos acima de 24 horas.

## Abstração: contrato genérico DAO

```java
public interface GenericDAO<T> {
    void inserir(T entidade) throws SQLException;
    T buscarPorId(int id) throws SQLException;
    List<T> listarTodos() throws SQLException;
    void atualizar(T entidade) throws SQLException;
    void excluir(int id) throws SQLException;
}
```

`MedicamentoDAO implements GenericDAO<Medicamento>` implementa as cinco operações com `@Override`. A interface padroniza o acesso à persistência sem colocar SQL nas entidades. `buscarPorId` retorna `null` quando não encontra o registro; `listarTodos` retorna lista vazia quando não há dados.

## Como abrir

É necessário **JDK 21 ou superior**. Extraia o ZIP inteiro antes de iniciar.

- **Windows:** dê dois cliques em `iniciar.bat`.
- **Linux/macOS:** abra um terminal na pasta do projeto e execute `sh iniciar.sh`.

Os arquivos de inicialização compilam o código e abrem a interface gráfica. Os comandos manuais continuam no README principal. O funcionamento existente com MySQL e fallback para SQLite foi mantido.

## Como executar os testes de modelos

Na raiz do projeto, após compilar a aplicação:

Windows:

```powershell
javac -encoding UTF-8 -cp "bin;lib/*" -d bin tests/ModelosTest.java
java -cp "bin;lib/*" ModelosTest
```

Linux/macOS:

```sh
javac -encoding UTF-8 -cp "bin:lib/*" -d bin tests/ModelosTest.java
java -cp "bin:lib/*" ModelosTest
```

Resultado esperado: `OK: 72 verificações concluídas.`

Os testes cobrem RN04, consumo inválido, estoque zerado, overflow, autonomia após mudanças nos três parâmetros, limites de 5 e 6 dias, intervalos que não dividem 24 e acima de um dia, encapsulamento das coleções, herança, confirmação de dose e as cinco operações declaradas no contrato DAO.

## Validação realizada

- Compilação completa dos modelos, DAOs, configuração, telas e ponto de entrada com Java 21.
- 72 verificações automatizadas de modelos e contrato DAO aprovadas.
- Integração em banco SQLite temporário: inserir, buscar, listar, atualizar, excluir, confirmar doses e atualizar o alerta. Confirmação duplicada e consumo sem estoque foram rejeitados, preservando os registros.
- Renderização das quatro páginas, janela no tamanho mínimo e formulários de cadastro e reposição para revisão visual.
- Inicialização e encerramento em modo CLI com SQLite.

A integração com um servidor MySQL ativo não foi executada nesta revisão.
