# ADR-002: Camada de Acesso a Dados com Padrão DAO e Interface Genérica

**Status:** Vigente  
**Data:** 2026-11-10  

---

## Contexto
Na 1ª Entrega, o padrão arquitetural definido foi o **MVC (Model-View-Controller)**. Contudo, a modelagem conceitual inicial concentrava regras de persistência dentro de métodos de entidades ou controllers, o que geraria alto acoplamento entre regras de negócio e queries SQL.

Além disso, a rubrica da 2ª Entrega exige obrigatoriamente:
1. O uso comprovado de **Classe Abstrata ou Interface**.
2. **CRUD Funcional Completo** operando sobre a entidade principal (`Medicamento`).
3. Respeito aos princípios de Separação de Responsabilidades (SoC) e Clean Code.

## Decisão
Introduzir formalmente o padrão **Data Access Object (DAO)** como subcamada de persistência dentro do MVC, estruturada da seguinte forma:
1. **Interface Genérica (`GenericDAO<T>`):** Define o contrato estrito para persistência relacional (`inserir`, `buscarPorId`, `listarTodos`, `atualizar`, `excluir`), assegurando o pilar de Abstração em POO.
2. **Implementações Especializadas:**
   - `MedicamentoDAO`: Implementa `GenericDAO<Medicamento>` contendo as operações completas de CRUD e ajuste manual de estoque.
   - `DoseDAO`: Gerencia agendamento, lembretes de doses e baixa atômica com controle de transação (`conn.setAutoCommit(false)`, `commit()` e `rollback()`).
   - `UsuarioDAO`: Gerencia persistência polimórfica de `Paciente` e `Responsavel` sob a estratégia Single Table.
3. **Uso de PreparedStatements e Try-with-Resources:** Todas as instruções SQL utilizam blocos de fechamento automático de conexões e declarações preparadas contra SQL Injection.

## Consequências
* **Positivas:**
  * Desacoplamento estrito: as classes de domínio (`Medicamento`, `Dose`, `Usuario`) permanecem focadas em regras de negócio e validações, sem dependência de classes do pacote `java.sql.*`.
  * Comprovação inequívoca do pilar de Interface e Abstração perante a banca avaliadora.
  * Facilidade de testes e garantia de atomicidade (ACID) na dedução do estoque físico durante a confirmação de doses.
* **Negativas / Limitações:**
  * Aumento no número de classes no projeto, o que é plenamente justificado pelo ganho em manutenibilidade e modularidade.
