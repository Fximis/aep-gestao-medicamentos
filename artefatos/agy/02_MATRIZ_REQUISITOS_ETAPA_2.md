# Matriz de Requisitos, Rubrica e Análise de Gaps — 2ª Entrega AEP (2026.2)

**Instituição:** UniCesumar  
**Cursos:** Engenharia de Software (Esoft) & Análise e Desenvolvimento de Sistemas (ADS)  
**Peso da Atividade:** 1,0 ponto na média final de todas as disciplinas do 4º Semestre  
**Data Limite Prevista no Cronograma:** 20/11/2026  
**Documento Base:** `Diretrizes Gerais da AEP (4º Semestre de 2026).pdf` + `docs/1a_entrega.md`

---

## 1. Rubrica Oficial de Avaliação da 2ª Entrega

Conforme estabelecido formalmente na Seção IV (página 7) do edital:

| Critério | Métrica de Aceitação Algorítmica/Visual | Peso |
| :--- | :--- | :---: |
| **1. Aderência ao Planejamento do Projeto** | O código entregue deve refletir os Requisitos e funcionalidades listados na 1ª entrega. Divergências do planejado configuram quebra de contrato de desenvolvimento. | **0,2** |
| **2. Pilares de POO** | Uso obrigatório de: <br>1) Classe Abstrata ou Interface; <br>2) Herança; <br>3) Polimorfismo via `@Override`. <br>Código fortemente tipado seguindo convenções (CamelCase). | **0,3** |
| **3. CRUD Funcional** | Operações completas de **Create**, **Read**, **Update** e **Delete** operando sobre a entidade principal definida no escopo da 1ª entrega (`Medicamento`). | **0,3** |
| **4. Persistência em BD** | **Conexão ativa com Banco de Dados**. Script SQL DDL de criação das tabelas na pasta `/database` perfeitamente compatível com as classes Java. | **0,2** |
| **TOTAL** | **Avaliação Somatória da 2ª Entrega** | **1,0** |

---

## 2. Cláusulas de Reprovação e Penalidades Estritas ("Armadilhas do Edital")

O documento institucional possui uma seção explícita (*"III. Fundamentação para Atribuição de Nota Baixa"*, pág. 5) e diretrizes adicionais (págs. 7 e 8) que a equipe deve cumprir rigorosamente:

> [!CAUTION]
> ### 1. Simulação de CRUD e Banco de Dados (Zero Imediato)
> *"Simulação de CRUD: se o banco de dados não estiver conectado (usar listas em memória), a nota de persistência é zero (0,2), sem meio-certo."*  
> **Impacto no projeto:** O protótipo atual em `feature/entrega-2/src/Main.java` opera exclusivamente com `ArrayList` e variáveis locais. **Se entregue assim, perde 0,2 de imediato.**

> [!WARNING]
> ### 2. Ausência de Qualquer Pilar de POO (Perda Total de 0,3)
> *"A ausência de um dos pilares de POO (ex: não usou Herança) acarretará na perda total dos pontos do critério, independente de o software estar bonitinho."*  
> O código deve ter obrigatoriamente: Classe Abstrata/Interface + Herança (`extends`) + Polimorfismo comprovado com anotação `@Override`.

> [!CAUTION]
> ### 3. Nota Zero Automática na 2ª Entrega
> O trabalho é sumariamente zerado se:
> 1. O software **não executar** na máquina do avaliador.
> 2. O código for puramente procedural (ignorar arquitetura orientada a objetos).
> 3. A entrega divergir 100% do escopo aprovado no 1º Bimestre.

> [!IMPORTANT]
> ### 4. Contribuição de TODOS no Repositório GitHub
> O edital exige expressamente: *"Contribuição de TODOS no repositório"*.
> Integrantes da Equipe:
> - **Fellipe Miquelão Schmidt** (RA: 25153218-2)
> - **Lucas Thomaz Brandel Zequini** (RA: 25169037-2)
> - **Carlos Henrique Saran Pappa** (RA: 25143592-2)
> **Ação obrigatória:** A branch ou PR da 2ª entrega deve conter commits assinados e vinculados às contas GitHub de cada um dos três membros.

> [!NOTE]
> ### 5. Independência de Interface Gráfica (Sistemas Operacionais & ETI)
> *"Não será cobrada interface gráfica avançada (ETI). A nota foca exclusivamente em: O projeto está fundamentado tecnicamente? O código funciona? Ele é orientado a objetos? Ele salva no banco?"*  
> Uma aplicação de console CLI limpa, modular e com menu interativo claro é **100% aderente**, livre de bugs de renderização gráfica e muito mais fácil de compilar em qualquer sistema operacional (respeitando a disciplina de SO).

---

## 3. Matriz de Rastreabilidade de Escopo (Contrato da 1ª Entrega)

Para garantir nota máxima (**0,2**) em **Aderência ao Planejamento**, o sistema Java deve contemplar cada item formalizado na 1ª entrega:

| Identificador | Requisito da 1ª Entrega | Implementação Necessária no Código da 2ª Fase |
| :--- | :--- | :--- |
| **RF01** | Cadastro de Medicamentos | `MedicamentoDAO.inserir()`: nome, estoque inicial, frequência em horas, quantidade por dose. |
| **RF02** | Sistema de Lembretes | Listagem das próximas doses do paciente com data/hora calculada. |
| **RF03** | Confirmação de Dose | Operação no menu/sistema onde o paciente confirma que tomou a dose programada. |
| **RF04** | Controle Automático de Estoque | Atualização no banco da quantidade em estoque do medicamento imediatamente após a confirmação (`RN01`, `RN06`). Recálculo dinâmico de autonomia em dias (`RN05`). |
| **RF05** | Alerta Proativo de Recompra | Verificação automática após a baixa: se $\text{diasRestantes} \le 5$, exibir destaque de alerta de reposição imediata (`RN03`). |
| **RF06** | Gestão Familiar | Exibição de visão supervisionada onde um `Responsavel` monitora o paciente associado. |
| **RF07** | Ajuste Manual de Estoque | Opção de edição direta da quantidade de estoque de um medicamento (`MedicamentoDAO.atualizarEstoque()`), com trava contra valores negativos (`RN04`, `RN07`). |

---

## 4. Análise de Gaps (Rascunho Atual vs. Requisitos da 2ª Entrega)

Comparação minuciosa entre o código existente na branch local `feature/entrega-2` e o estado exigido para nota máxima:

| Componente | Estado em `feature/entrega-2` | O que falta para a Aprovação Completa (Nota 1,0) |
| :--- | :--- | :--- |
| **Entidades POO** | Classes `Usuario`, `Paciente`, `Responsavel`, `Medicamento`, `Dose` criadas. | Ajustar getters, construtores e tipagem de datas (`LocalDateTime` / `Timestamp`) para compatibilidade total com o banco. |
| **Pilares de POO** | `Usuario` abstrato, subclasses usam `extends` e `@Override` em `obterTipo()`. | **Aprovado.** Recomenda-se também criar uma `interface GenericDAO<T>` para consagrar o pilar de abstração/interface. |
| **Conexão com Banco** | **Nenhuma**. Código roda apenas em memória. | **GAP CRÍTICO:** Criar classe `DatabaseConnection` gerenciando conexão JDBC real (`DriverManager.getConnection`). |
| **Banco de Dados (SGBD)** | Arquivo `database/schema.sql` modelado para MySQL. | Manter `schema.sql` na pasta `/database` e garantir driver JDBC compatível (MySQL Connector/J ou SQLite). |
| **Camada DAO (CRUD)** | Inexistente. | **GAP CRÍTICO:** Criar `MedicamentoDAO` com os métodos: <br>- `create(Medicamento m)` <br>- `findById(int id)` / `findAll()` <br>- `update(Medicamento m)` <br>- `delete(int id)` |
| **Interface com Usuário** | `Main.java` roda uma simulação estática e encerra. | **GAP CRÍTICO:** Criar interface interativa no terminal (CLI via `Scanner`) permitindo cadastrar, listar, atualizar estoque, excluir remédio e confirmar doses em tempo real. |
| **Instruções de Execução** | Ausentes (README não tem guia de compilação da fase 2). | **GAP DE SO:** Documentar no `README.md` como executar o script SQL, compilar os arquivos Java e rodar o projeto no terminal via PowerShell/CMD. |
| **Distribuição no Git** | Código não submetido ao GitHub; nenhum commit de Lucas ou Carlos. | **GAP DE EQUIPE:** Enviar branch, abrir PR e distribuir commits com autoria dos três integrantes. |
