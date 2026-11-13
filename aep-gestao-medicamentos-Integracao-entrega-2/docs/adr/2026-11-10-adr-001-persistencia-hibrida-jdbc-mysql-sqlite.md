# ADR-001: Persistência Relacional Híbrida via JDBC (MySQL com Fallback SQLite)

**Status:** Vigente  
**Data:** 2026-11-10  

A criação de contas e a migração de credenciais seguem o [ADR-005](2026-11-11-adr-005-autenticacao-e-controle-de-acesso.md).

---

## Contexto
Na documentação conceitual da 1ª Entrega (`docs/1a_entrega.md`), foi previsto o uso genérico de um "Banco de Dados SQL Relacional (MySQL ou PostgreSQL)".

Ao avançar para a 2ª Entrega (Implementação e Integração), o edital da instituição impôs restrições rigorosas:
1. **Nota Zero Automática:** Caso o software não execute na máquina do avaliador.
2. **Nota Zero em Persistência (sem meio-certo):** Caso o software execute com listas em memória (simulação de CRUD) em vez de uma conexão ativa com banco de dados relacional.
3. **Heterogeneidade de Ambientes:** Diferentes membros da equipe e avaliadores podem não possuir o serviço local do MySQL ativo ou configurado na mesma porta e credenciais padrão.

## Decisão
Implementar uma fábrica de conexões centralizada (`DatabaseConnection.java`) baseada em **JDBC Puro** (Java Standard Edition), com resiliência de ambiente em duas camadas:
1. **Conexão Primária (MySQL):** O sistema tenta conectar em `localhost:3306/gestao_medicamentos` utilizando o driver oficial `mysql-connector-j-8.4.0.jar`.
2. **Fallback Automático (SQLite):** Caso o serviço MySQL esteja inacessível, o sistema ativa transparentemente a conexão JDBC relacional via `sqlite-jdbc-3.46.1.3.jar` apontando para `database/gestao_medicamentos.db`. O esquema DDL e a carga inicial (*Seed*) são inicializados automaticamente.
3. **Distribuição dos Drivers:** Os arquivos `.jar` dos drivers JDBC ficam versionados no diretório `lib/`, eliminando a necessidade de gerenciadores de dependência externos como Maven ou Gradle.

## Consequências
* **Positivas:**
  * Conformidade integral com o critério de *Persistência em BD* (0,2), garantindo conexão JDBC ativa em qualquer ambiente de avaliação.
  * Execução instantânea e sem atrito para qualquer integrante da equipe ou professor, eliminando dependências externas pré-instaladas.
  * Preservação das propriedades ACID, integridade referencial (FKs e constraints) e compatibilidade com o script DDL em `database/schema.sql`.
* **Negativas / Limitações:**
  * O arquivo `.jar` do SQLite (~14 MB) é mantido no repositório na pasta `lib/` para garantir autonomia completa de execução offline.
