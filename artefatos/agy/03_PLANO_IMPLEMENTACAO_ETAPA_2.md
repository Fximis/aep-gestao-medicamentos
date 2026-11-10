# Plano de Implementação — 2ª Entrega da AEP (Software Funcional Integrado)

> **Destinado a:** Equipe de Desenvolvimento e Agentes de Execução.  
> **Metodologia:** MVC Modular + Data Access Object (DAO) com JDBC Puro (KISS/YAGNI).  
> **Data de Entrega Institucional:** 20/11/2026.

---

## 1. Visão Geral da Arquitetura

```text
aep-gestao-medicamentos/
├── database/
│   ├── README.md                      # Instruções de setup do banco de dados
│   └── schema.sql                     # Script DDL com constraints, FKs e dados iniciais (Seed)
├── lib/
│   └── mysql-connector-j-8.4.0.jar    # Driver JDBC (ou sqlite-jdbc caso utilize banco embutido)
├── src/
│   ├── config/
│   │   └── DatabaseConnection.java    # Singleton/Factory de conexão JDBC ativa
│   ├── dao/
│   │   ├── GenericDAO.java            # Interface genérica de persistência (Pilar de Abstração)
│   │   ├── UsuarioDAO.java            # Persistência de Paciente e Responsável (Single Table)
│   │   ├── MedicamentoDAO.java        # CRUD completo da entidade principal (Create, Read, Update, Delete)
│   │   └── DoseDAO.java               # Registro de agendamento e confirmação de consumo
│   ├── models/
│   │   ├── Usuario.java               # Classe Abstrata base (id, nome, email, senha)
│   │   ├── Paciente.java              # Subclasse herdeira com método polimórfico @Override
│   │   ├── Responsavel.java           # Subclasse herdeira com método polimórfico @Override
│   │   ├── Medicamento.java           # Entidade de domínio: autonomia, estoque e cálculo de dias
│   │   └── Dose.java                  # Entidade de agendamento e histórico de consumo
│   ├── views/
│   │   └── ConsoleView.java           # Menus e formatação de saídas CLI para o usuário/avaliador
│   └── Main.java                      # Ponto de entrada, orquestrador do loop do menu e bootstrap
└── README.md                          # Instruções completas de compilação e execução (SO)
```

---

## 2. Decisões Arquiteturais e Trade-offs

1. **Por que JDBC Puro (Java Standard Library)?**
   - **KISS & YAGNI:** Adicionar frameworks pesados (Spring Boot, Hibernate/JPA) introduziria dezenas de dependências externas, arquivos de build complexos (Maven/Gradle) e risco de falha de inicialização na máquina do professor.
   - **Didática e Transparência:** O professor avaliará se os alunos entendem SQL, transações, abertura e fechamento de conexões (`PreparedStatement`, `ResultSet`, `try-with-resources`). O JDBC puro comprova domínio dos fundamentos de Banco de Dados e POO.
2. **Interface CLI (Terminal Interativo) vs Interface Gráfica:**
   - O edital deixa claro: *"Não será cobrada interface gráfica avançada (ETI). A nota foca exclusivamente em: O projeto está fundamentado tecnicamente? O código funciona? Ele é orientado a objetos? Ele salva no banco?"*.
   - Uma interface gráfica Swing/JavaFX pode falhar em servidores sem display gráfico ou requerer módulos adicionais (JavaFX SDK) no Java 21 LTS. A CLI via terminal é 100% à prova de falhas em qualquer sistema operacional.
3. **Estratégia de Banco de Dados:**
   - **Banco Principal:** MySQL 8.x (compatível com a instalação padrão do Laragon do ambiente do usuário).
   - **Facilidade:** Um script de seed (`schema.sql`) insere dados de exemplo para que o avaliador possa rodar consultas e testes imediatamente sem ter que cadastrar tudo do zero.

---

## 3. Tarefas de Implementação Passo a Passo

### Tarefa 1: Camada de Conexão com Banco de Dados e Script DDL
**Objetivo:** Garantir conexão ativa com o SGBD e eliminar qualquer risco de nota zero em persistência.

**Arquivos:**
- `database/schema.sql` (Ajustar e incluir comandos de `INSERT` de teste / Seed)
- `src/config/DatabaseConnection.java` (Criar classe de conexão JDBC)
- `lib/` (Adicionar o driver JDBC do banco escolhido)

**Checklist:**
- [ ] Criar a pasta `lib/` e incluir o driver JDBC (`mysql-connector-j.jar`).
- [ ] Atualizar `database/schema.sql` com:
  - Tabela `usuario` (com coluna `tipo` discriminando `'PACIENTE'` ou `'RESPONSAVEL'`).
  - Tabela `medicamento` (com FK para `usuario_id`).
  - Tabela `dose` (com FK para `medicamento_id`).
  - Cargas iniciais (`INSERT`) de teste para agilizar a demonstração.
- [ ] Criar `src/config/DatabaseConnection.java` com:
  - Constantes configuráveis de URL (`jdbc:mysql://localhost:3306/gestao_medicamentos`), usuário (`root`) e senha.
  - Método `public static Connection getConnection() throws SQLException`.
  - Tratamento claro de `ClassNotFoundException` informando o caminho do driver JAR caso falte no classpath.
- [ ] Testar a conexão abrindo e fechando com sucesso via console.

---

### Tarefa 2: Refinamento dos Modelos de Domínio e Pilares de POO
**Objetivo:** Consolidar 100% dos pilares de POO (Classe Abstrata, Interface, Herança e Polimorfismo com `@Override`).

**Arquivos:**
- `src/models/Usuario.java`
- `src/models/Paciente.java`
- `src/models/Responsavel.java`
- `src/models/Medicamento.java`
- `src/models/Dose.java`
- `src/dao/GenericDAO.java` (Interface de abstração)

**Checklist:**
- [ ] Criar interface `src/dao/GenericDAO.java`:
  ```java
  public interface GenericDAO<T> {
      void inserir(T entidade) throws SQLException;
      T buscarPorId(int id) throws SQLException;
      List<T> listarTodos() throws SQLException;
      void atualizar(T entidade) throws SQLException;
      void excluir(int id) throws SQLException;
  }
  ```
- [ ] Garantir que `Usuario` seja abstrata com método abstrato `public abstract String obterTipo();`.
- [ ] Garantir que `Paciente` e `Responsavel` utilizem `extends Usuario` e anotem `@Override` explicitamente no método `obterTipo()`.
- [ ] Em `Medicamento.java`, consolidar:
  - `calcularDiasRestantes()`: $( \text{quantidadeEstoque} / (\frac{24}{\text{frequenciaHoras}} \times \text{quantidadePorDose}) )$.
  - `verificarAlertaRecompra()`: retorna `true` se $\text{calcularDiasRestantes()} \le 5$.
  - Proteção de encapsulamento: getters, setters e validação de quantidade não negativa (`RN04`).
- [ ] Em `Dose.java`, estruturar construtores com `LocalDateTime`, `statusTomado` e associação ao `Medicamento`.

---

### Tarefa 3: CRUD Completo da Entidade Principal (`MedicamentoDAO`)
**Objetivo:** Atender integralmente ao critério "CRUD Funcional" (Peso 0,3) com operações Create, Read, Update e Delete operando sobre o banco de dados.

**Arquivos:**
- `src/dao/MedicamentoDAO.java` (Implementa `GenericDAO<Medicamento>`)

**Checklist:**
- [ ] **Create (`inserir`):** Executa `INSERT INTO medicamento (usuario_id, nome, quantidade_estoque, frequencia_horas, quantidade_por_dose) VALUES (?, ?, ?, ?, ?)`. Recupera o ID gerado via `Statement.RETURN_GENERATED_KEYS`.
- [ ] **Read (`buscarPorId` e `listarTodos`):** Executa `SELECT * FROM medicamento WHERE id = ?` e `SELECT * FROM medicamento ORDER BY nome`. Converte `ResultSet` em instâncias de `Medicamento`.
- [ ] **Update (`atualizar` e `atualizarEstoque`):** Executa `UPDATE medicamento SET nome = ?, quantidade_estoque = ?, frequencia_horas = ?, quantidade_por_dose = ? WHERE id = ?`. Método específico para ajuste manual de estoque (`RF07`).
- [ ] **Delete (`excluir`):** Executa `DELETE FROM medicamento WHERE id = ?` (com exclusão em cascata das doses associadas ou trava de integridade).
- [ ] Garantir que todos os métodos utilizem `PreparedStatement` e `try-with-resources` para evitar SQL Injection e vazamento de conexões.

---

### Tarefa 4: DAOs Complementares e Regras de Negócio Chave
**Objetivo:** Cumprir o ciclo fechado de agendamento de doses, confirmação ativa e alerta de recompra.

**Arquivos:**
- `src/dao/UsuarioDAO.java`
- `src/dao/DoseDAO.java`

**Checklist:**
- [ ] Em `UsuarioDAO.java`:
  - `listarPacientes()` e `buscarPorEmail()` para simulação de login/troca de perfil.
- [ ] Em `DoseDAO.java`:
  - `gerarDosesIniciais(Medicamento m)`: cria os registros de doses futuras para as próximas 24h ou 48h com base na frequência em horas (`horario_previsto`).
  - `listarDosesPendentes(int pacienteId)`: lista doses com `status_tomado = FALSE`.
  - `confirmarDose(int doseId, Medicamento m)`:
    - Executa transação bancária (`conn.setAutoCommit(false)`):
      1. Marca `status_tomado = TRUE` e grava `horario_consumo = NOW()`.
      2. Deduz `quantidade_por_dose` da tabela `medicamento`.
      3. Efetua `commit()`.
    - Executa verificação do alerta: se `medicamento.verificarAlertaRecompra()`, emite aviso sonoro/destacado de compra (`RF05`).

---

### Tarefa 5: Interface de Linha de Comando (CLI) e Ponto de Entrada (`Main.java`)
**Objetivo:** Permitir que o professor ou avaliador execute o sistema interativamente através de um menu intuitivo, testando cada Requisito Funcional com facilidade.

**Arquivos:**
- `src/views/ConsoleView.java`
- `src/Main.java`

**Estrutura do Menu do Terminal:**
```text
=====================================================
  SISTEMA DE GESTÃO DE MEDICAMENTOS CONTÍNUOS (AEP)
=====================================================
1. Cadastrar Medicamento (Create)
2. Listar Medicamentos e Autonomia em Dias (Read)
3. Atualizar Dados do Medicamento (Update)
4. Ajustar Manualmente o Estoque (Update de Estoque)
5. Excluir Medicamento (Delete)
-----------------------------------------------------
6. Agenda de Doses do Paciente (Lembretes)
7. Confirmar Tomada de Dose (Baixa e Alerta de 5 Dias)
8. Visão do Cuidador / Responsável (Gestão Familiar)
-----------------------------------------------------
0. Sair do Sistema
Escolha uma opção: 
```

**Checklist:**
- [ ] Implementar loop com tratamento de exceções (evitar que o programa quebre se o usuário digitar uma letra no lugar de um número).
- [ ] Exibir mensagens coloridas ou com formatação destacada (`[ALERTA DE RECOMPRA]`, `[SUCESSO]`, `[ERRO]`).
- [ ] Disponibilizar a visualização polimórfica: exibir o usuário ativo e o resultado de `usuario.obterTipo()`.

---

### Tarefa 6: Documentação de Execução e Sistemas Operacionais (`README.md`)
**Objetivo:** Atender à exigência de "Ambiente de Sistemas Operacionais" do edital (página 8, item 3).

**Arquivos:**
- `README.md` (Atualizar seção de execução e tecnologias)
- `database/README.md` (Passo a passo para importar o `schema.sql`)

**Checklist:**
- [ ] Descrever os pré-requisitos: Java JDK 21 LTS e MySQL (ou Laragon).
- [ ] Fornecer os comandos exatos de compilação no terminal:
  ```powershell
  # Windows PowerShell
  javac -cp "lib/*;src" -d bin src/models/*.java src/config/*.java src/dao/*.java src/views/*.java src/Main.java
  java -cp "bin;lib/*" Main
  ```
- [ ] Fornecer o equivalente para Linux/macOS (substituindo `;` por `:` no classpath).
- [ ] Incluir dados de login ou opções de teste rápido para o avaliador.

---

### Tarefa 7: Estratégia de Versionamento Git e Contribuição Coletiva
**Objetivo:** Cumprir o requisito institucional de *"Contribuição de TODOS no repositório"*.

**Integrantes:**
1. **Fellipe Miquelão Schmidt** (RA: 25153218-2)
2. **Lucas Thomaz Brandel Zequini** (RA: 25169037-2)
3. **Carlos Henrique Saran Pappa** (RA: 25143592-2)

**Divisão de Responsabilidades sugerida nos Commits:**
- **Fellipe:** Arquitetura central, `DatabaseConnection`, `schema.sql` e CRUD em `MedicamentoDAO`.
- **Lucas:** Modelagem POO refinada (`models/*`), `GenericDAO` e camada de testes/validação de regras de negócio.
- **Carlos:** `DoseDAO`, regras de confirmação de dose com transação, `ConsoleView` e documentação de execução.

**Procedimento de Entrega no Git:**
1. Mesclar a base da `feature/entrega-2` com a `master` atualizada.
2. Cada integrante faz o clone ou checkout da branch de trabalho (`feature/entrega-2`).
3. Cada integrante efetua seus respectivos commits utilizando seu próprio `git config user.name` e `git config user.email` associado à sua conta do GitHub.
4. Abrir o Pull Request no GitHub: `feature/entrega-2` -> `master`.
5. Fazer a revisão e merge final para a `master`, garantindo que o gráfico de contribuidores do GitHub apresente commits dos três membros.

---

## 4. Checklist de Verificação Pré-Envio (Garantia de Nota 10)

- [ ] **Execução sem erros:** O comando `java` roda perfeitamente do início ao fim sem lançar unhandled exceptions.
- [ ] **Banco Conectado:** O banco de dados grava e lê registros reais (provado via `SELECT * FROM medicamento` no MySQL Workbench / HeidiSQL / terminal).
- [ ] **Herança e Polimorfismo:** `Usuario` abstrato com subclasses `Paciente` e `Responsavel` com método `obterTipo()` anotado com `@Override`.
- [ ] **CRUD 100%:** É possível criar, listar, editar e deletar medicamentos diretamente pelo console.
- [ ] **Regra dos 5 Dias:** A confirmação de doses reduz o estoque e dispara o alerta imediato quando a autonomia for $\le 5$ dias.
- [ ] **README Atualizado:** Instruções claras de como rodar o projeto em qualquer máquina.
- [ ] **Commits Coletivos:** Os 3 integrantes constam no histórico de commits do GitHub.
