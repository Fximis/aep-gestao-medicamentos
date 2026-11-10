# ADR-003: Interface Gráfica Desktop (Java Swing GUI) Humanizada com Painel Técnico Integrado

**Status:** Vigente  
**Data:** 2026-11-10  

---

## Contexto
Na 1ª Entrega, a etapa de Descoberta apontou como público-alvo prioritário idosos, cuidadores familiares e pessoas com rotinas intensas de medicação contínua. Paralelamente, o edital da instituição exigiu um sistema funcional e integrado para a 2ª fase.

A utilização exclusiva de terminal de linha de comando (CLI) impõe uma barreira de usabilidade para usuários leigos e idosos (público-alvo primário do ODS 3 da ONU). Uma aplicação desktop gráfica acessível, com botões claros, alertas visuais coloridos e tabelas responsivas atende de forma superior aos Requisitos Não Funcionais de Usabilidade (**RNF01**).

## Decisão
Implementar uma interface gráfica desktop interativa nativa utilizando **Java Swing** (`javax.swing.*` e `java.awt.*`), padronizada sob o nome **CUIDADO DIÁRIO**:
1. **Padrão Nativo do Sistema:** Aplicação do System Look and Feel (`UIManager.getSystemLookAndFeelClassName()`), integrando-se visualmente ao sistema operacional (Windows/Linux/macOS) sem dependências externas pesadas.
2. **Organização por Abas Temáticas:**
   - **Aba 1 (🕒 Minha Rotina - Doses):** Exibe a lista de horários de hoje com destaque visual para doses pendentes e botões para confirmação individual ou em lote (`⚡ Tomar Todas as Doses Agora`).
   - **Aba 2 (💊 Meus Medicamentos & Estoque):** Tabela completa de controle com autonomia calculada em dias, status de alerta e botões para novo cadastro, edição, reposição de caixas novas e exclusão com diálogo de confirmação.
   - **Aba 3 (👨‍👩‍👧 Espaço da Família / Cuidador):** Dashboard com barra de progresso visual de adesão diária e dados do acompanhante (RF06 / RN08).
   - **Aba 4 (⚙️ Diretrizes Técnicas - Banca AEP):** Painel reservado para a banca avaliadora inspecionar o status da conexão JDBC ativa, integridade referencial do DDL e chamadas polimórficas de POO com `@Override`.
3. **Alertas Visuais Proativos:** Popups modais (`JOptionPane`) disparados automaticamente sempre que a confirmação de uma dose reduz o estoque a $\le 5$ dias de autonomia (RF05 / RN03).
4. **Resiliência e Fallback:** Preservação do modo console CLI como alternativa via argumento `--cli` para servidores ou execuções sem display gráfico.

## Consequências
* **Positivas:**
  * Interface visual completa, intuitiva e amigável para qualquer pessoa comum ou idoso.
  * Execução instantânea utilizando a biblioteca padrão do Java SE 21, sem necessidade de módulos JavaFX adicionais ou dependências complexas.
  * Rastreabilidade total: a banca avaliadora pode interagir graficamente com todos os requisitos de POO, CRUD e Persistência através de uma interface limpa.
* **Negativas / Limitações:**
  * O código da camada View passa a gerenciar componentes Swing (`JTable`, `DefaultTableModel`, diálogos modais), o que é devidamente isolado no pacote `views.gui`.
