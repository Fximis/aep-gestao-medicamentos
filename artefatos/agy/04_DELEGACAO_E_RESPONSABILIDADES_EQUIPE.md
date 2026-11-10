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

Caso o professor solicite apresentação ou arguição do código:
1. **Mostrar o script SQL:** Apresentar as tabelas em `database/schema.sql` com PK, FK e constraints.
2. **Mostrar a Conexão JDBC:** Apresentar `src/config/DatabaseConnection.java` provando que não há simulação em memória.
3. **Demonstrar os Pilares de POO:**
   - Abstração: `Usuario.java` (classe abstrata) e `GenericDAO.java` (interface).
   - Herança: `Paciente extends Usuario` e `Responsavel extends Usuario`.
   - Polimorfismo: `obterTipo()` sobrescrito com `@Override` retornando comportamentos distintos.
4. **Demonstrar o CRUD:** Executar o menu criando um medicamento, listando com o cálculo de dias, alterando o estoque e excluindo.
5. **Demonstrar a Regra dos 5 Dias:** Confirmar doses até o estoque atingir $\le 5$ dias de autonomia e mostrar o alerta proativo sendo disparado no terminal.
