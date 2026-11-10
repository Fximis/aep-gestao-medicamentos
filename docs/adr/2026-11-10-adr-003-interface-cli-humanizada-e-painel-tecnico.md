# ADR-003: Interface CLI Humanizada (Cuidado Diário) com Painel Técnico Reservado

**Status:** Vigente  
**Data:** 2026-11-10  

---

## Contexto
Na 1ª Entrega, a etapa de Descoberta apontou como público-alvo prioritário idosos, cuidadores e pessoas com rotinas intensas de medicação contínua. Paralelamente, o edital da instituição esclareceu nas Orientações Finais que:
1. *"Não será cobrada interface gráfica avançada (ETI). A nota foca exclusivamente em: O projeto está fundamentado tecnicamente? O código funciona? Ele é orientado a objetos? Ele salva no banco?"*.
2. As primeiras versões de consoles CLI tendiam a rotular as opções de menu utilizando jargões estritamente acadêmicos (`[1] Create`, `[2] Read`, `[9] Polimorfismo`), o que contradiz o propósito de valor centrado no usuário real.

## Decisão
Adotar uma interface de linha de comando (CLI) baseada em terminal console (`ConsoleView.java`) com os seguintes princípios de design:
1. **Linguagem Natural e Acolhedora:** Menus estruturados sob a perspectiva da rotina diária (*"Tomar Medicamento Agora"*, *"Minha Rotina de Remédios"*, *"Repor Estoque"* e *"Espaço da Família"*).
2. **Dashboard Imediato no Topo:** A tela principal informa logo na inicialização se há doses atrasadas ou pendentes para o momento atual e destaca em alerta visual caso haja medicamentos com estoque crítico ($\le 5$ dias de autonomia).
3. **Tomada de Doses Eficiente (Suporte a Lote `[T]`):** Permite confirmação de doses específicas ou a confirmação de todos os remédios daquele horário em uma única ação transacional.
4. **Painel Técnico Reservado para a Banca:** Opção dedicada `[8]` que executa testes de reflexão e itera sobre a coleção polimórfica `List<Usuario>` comprovando o `@Override` do método `obterTipo()`, detalhes de conexão JDBC e estrutura do banco, sem poluir a experiência da pessoa comum.
5. **Compatibilidade Universal de Terminal:** Utilização exclusiva de caracteres universais ASCII e codificação UTF-8, prevenindo caracteres truncados (*mojibake*) em ambientes Windows (PowerShell / Prompt de Comando).

## Consequências
* **Positivas:**
  * Experiência de uso intuitiva, realista e aderente à proposta de valor do projeto.
  * Execução rápida, livre de falhas de renderização de janelas gráficas em diferentes resoluções ou sistemas operacionais.
  * Atendimento duplo: satisfaz os requisitos de usabilidade para o usuário leigo e fornece acesso imediato às evidências de conformidade para o professor.
* **Negativas / Limitações:**
  * Ausência de interface visual com botões e formulários gráficos (opção deliberada e amparada pelo item 1 das orientações finais do edital da AEP).
