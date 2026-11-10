# Delegação de Responsabilidades e Contribuição em Equipe — AEP 4º Semestre

**Objetivo:** Atender ao requisito expresso do edital: *"Contribuição de TODOS no repositório"* (Página 4, 2ª Entrega).  
**Integrantes:**
1. **Fellipe Miquelão Schmidt** (R.A.: `25153218-2`)
2. **Lucas Thomaz Brandel Zequini** (R.A.: `25169037-2`)
3. **Carlos Henrique Saran Pappa** (R.A.: `25143592-2`)

---

## 1. Matriz de Atividades por Integrante

| Integrante | Pacotes / Arquivos | Responsabilidades Técnicas | Evidência de Contribuição no Git |
| :--- | :--- | :--- | :--- |
| **Fellipe Schmidt** | `database/schema.sql`<br>`src/config/DatabaseConnection.java`<br>`src/dao/MedicamentoDAO.java`<br>`src/Main.java` | - Arquitetura geral e padronização do projeto.<br>- Conexão JDBC ativa com o banco MySQL.<br>- Implementação do CRUD completo de `Medicamento` (Create, Read, Update, Delete).<br>- Gerenciamento da branch e integração das partes. | Commit: `feat(db): conexao ativa JDBC e CRUD completo de medicamentos` |
| **Lucas Thomaz** | `src/models/*`<br>`src/dao/GenericDAO.java`<br>`src/models/Medicamento.java` | - Garantia dos 4 pilares de POO (Abstração, Herança, Polimorfismo com `@Override` e Encapsulamento).<br>- Interface `GenericDAO<T>`.<br>- Lógica matemática de autonomia em dias e gatilho de recompra ($\le 5$ dias).<br>- Validações de integridade de domínio (estoque não negativo). | Commit: `feat(models): implementacao dos pilares de POO, interface GenericDAO e regra de alerta de 5 dias` |
| **Carlos Henrique** | `src/dao/DoseDAO.java`<br>`src/dao/UsuarioDAO.java`<br>`src/views/ConsoleView.java`<br>`README.md` | - Lógica de agendamento e baixa de doses com transação bancária.<br>- Consulta de pacientes por responsável (gestão familiar).<br>- Menu interativo no terminal (CLI via `Scanner`).<br>- Documentação das instruções de compilação e execução no README (disciplina de SO). | Commit: `feat(view): criacao do menu interativo CLI e integracao do fluxo de confirmacao de doses` |

---

## 2. Orientações Técnicas para o Lucas e para o Carlos

### 2.1. Como Configurar a Identidade no Git
Cada integrante deve garantir que seus commits sejam associados à sua conta oficial do GitHub:
```powershell
# No terminal da máquina do integrante:
git config user.name "Nome Completo"
git config user.email "email-cadastrado-no-github@exemplo.com"
```

### 2.2. Diretrizes de Código
- **Tipagem Estrita:** Nada de tipos primitivos sem validação; utilizar `LocalDateTime` para datas/horas.
- **Convenção CamelCase:**
  - Classes e Interfaces: `UpperCamelCase` (ex: `MedicamentoDAO`, `DatabaseConnection`).
  - Métodos e Variáveis: `lowerCamelCase` (ex: `calcularDiasRestantes`, `quantidadeEstoque`).
  - Constantes: `UPPER_SNAKE_CASE` (ex: `DIAS_ALERTA_RECOMPRA`).
- **Prevenção de Falhas:** Tratamento de `SQLException` com mensagens amigáveis no console; utilização de `try-with-resources` para fechar conexões, `PreparedStatement` e `ResultSet`.

---

## 3. Roteiro de Demonstração para a Banca / Professor

O sistema foi preparado tanto para uma pessoa comum (fluxo fluido de [1] a [7]) quanto para inspeção técnica rigorosa da banca:
1. **Opção [8] do Menu (Atalho para a Banca):** Pressione `8` no menu principal. O sistema exibirá uma tela consolidada comprovando em tempo real:
   - Os 4 pilares de POO (com iteração polimórfica e anotação `@Override`).
   - Persistência em banco de dados relacional (conexão JDBC ativa e transações ACID).
   - CRUD completo operando sobre a entidade `Medicamento`.
   - Rastreabilidade de todos os requisitos funcionais e regras de negócio da 1ª entrega.
2. **Mostrar o script SQL:** Apresentar as tabelas em `database/schema.sql` com PK, FK e constraints de integridade referencial.
3. **Demonstrar a Usabilidade Humana:**
   - Mostrar o **Dashboard do Dia** na inicialização (alertas prioritários imediatos).
   - Confirmar uma dose com **Opção [1]**: o paciente seleciona o remédio da lista e a baixa é realizada no estoque físico.
   - Reabastecer o estoque com **Opção [4]** adicionando uma nova caixa comprada na farmácia ($+30$ comprimidos) e verificando a autonomia subir dinamicamente.
