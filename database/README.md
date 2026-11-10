# Diretrizes de Banco de Dados — 2ª Entrega da AEP

Este diretório contém os artefatos de persistência relacional do sistema de gestão de medicamentos.

---

## 🗄️ Estrutura e Arquivos

* **`schema.sql`**: Script DDL completo contendo:
  - Criação da base de dados relacional (`gestao_medicamentos`).
  - Criação das tabelas com integridade referencial (Primary Keys, Foreign Keys, `ON DELETE CASCADE` e constraints):
    - `usuario`: Tabela com herança via Single Table Pattern (`tipo` discrimina `PACIENTE` e `RESPONSAVEL`).
    - `medicamento`: Tabela principal com estoque físico, frequência de doses e dosagem.
    - `dose`: Tabela de agendamento de doses, horários de consumo e confirmação.
  - Carga inicial de dados (*Seed*) para agilizar a validação e demonstração.

---

## ⚙️ Modos de Conexão Suportados (JDBC)

O sistema foi concebido com resiliência total para o ambiente de avaliação:

1. **MySQL 8.x (Padrão Recomendado):**
   - Configurado para `localhost:3306`, usuário `root` e senha em branco (padrão de ferramentas como Laragon e XAMPP).
   - Para importar o schema no MySQL:
     ```powershell
     mysql -u root -p < database/schema.sql
     ```
2. **Banco Relacional Embutido (SQLite):**
   - Se o servidor MySQL não estiver em execução, o sistema ativa automaticamente a conexão JDBC via SQLite (`database/gestao_medicamentos.db`), inicializando as tabelas e dados de teste sem que o avaliador precise instalar ou configurar serviços de banco de dados.
