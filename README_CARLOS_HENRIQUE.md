# Segunda entrega da AEP - contribuicao de Carlos Henrique

Este pacote complementa o ZIP `aep-gestao-medicamentos-feature-entrega-2.zip`.
**Ele nao e um projeto completo isolado.** Copie seu conteudo sobre a raiz do projeto
original, mantendo os mesmos caminhos. Nao apague os arquivos que ja existem.

## O que copiar

| Arquivo | Finalidade |
| --- | --- |
| `src/dao/DoseDAO.java` | Agenda, pendencias e confirmacao transacional com baixa de estoque |
| `src/views/ConsoleView.java` | Menu com as opcoes 1 a 7, CRUD e painel do cuidador |
| `src/Main.java` | Faz `Main --cli` abrir o novo menu e preserva a entrada da GUI |
| `scripts/executar-carlos.ps1` | Compila e abre o console no Windows |
| `scripts/executar-carlos.sh` | Compila e abre o console em Linux com Bash |
| `scripts/testar-carlos.ps1` | Executa testes com banco separado no Windows |
| `tests/CarlosHenriqueIntegrationTest.java` | Testes de integracao com SQLite real |
| `docs/entrega-2-carlos-henrique.md` | Contratos com Lucas, rastreabilidade e evidencias |

Os modelos de `src/models/`, a interface `src/dao/GenericDAO.java`,
`MedicamentoDAO`, `UsuarioDAO`, `DatabaseConnection`, os drivers de `lib/`
e o schema original **nao sao substituidos por este pacote**.
O trabalho do Lucas permanece separado. A compatibilidade foi validada com as
assinaturas existentes no ZIP recebido; alinhem essas assinaturas antes de integrar
novas alteracoes dele.

## Como testar no Windows

Requisito: JDK 21 ou superior, com `java` e `javac` no PATH.
Depois de copiar os arquivos, abra o PowerShell na raiz do projeto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\executar-carlos.ps1
```

O projeto original tenta MySQL e, se indisponivel na primeira conexao, usa
SQLite em `database/gestao_medicamentos.db`. Sao bancos relacionais reais via JDBC,
sem simular persistencia com listas. Mantenha os dois JARs que ja vieram em `lib/`.
Se usar MySQL, execute o `database/schema.sql` original antes e confira a configuracao
em `src/config/DatabaseConnection.java`.

No SQLite, o inicializador existente cria a estrutura e os dados demonstrativos.
O `schema.sql` original usa sintaxe de MySQL; nao o importe diretamente no SQLite.
A sessao do console usa os pacientes realmente cadastrados; se houver mais de um,
permite selecionar. Nao foram criados login nem novas tabelas de permissoes.

Para executar os testes automaticos:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\testar-carlos.ps1
```

Os testes usam um novo diretorio temporario e nao alteram seu banco da demonstracao.
Resultado verificado: **42 verificacoes aprovadas com SQLite real e JDK 21**.
MySQL e a interacao grafica nao foram executados nesta validacao; as fontes da GUI
foram compiladas junto com o restante do projeto.

## Como executar em Linux

Com JDK 21 instalado, na raiz do projeto:

```bash
bash scripts/executar-carlos.sh
```

O script Bash foi fornecido para compatibilidade; a execucao validada foi no Windows.

## Menu entregue

1. Cadastrar Medicamento (Create).
2. Listar Medicamentos e Autonomia (Read).
3. Atualizar Medicamento (Update).
4. Ajustar Estoque Manualmente (Update de Estoque).
5. Excluir Medicamento (Delete).
6. Confirmar Dose Tomada (Baixa + Alerta se <= 5 dias).
7. Visao do Responsavel / Cuidador.
0. Sair.

Selecoes de medicamento e dose usam o **ID mostrado na tela**, e nao a posicao na lista.
Todos os fluxos de alteracao pedem confirmacao.
A opcao 4 substitui o total fisico do estoque; ela nao soma uma compra automaticamente.
Na opcao 3, ENTER conserva cada valor atual. Mudanca de posologia refaz as doses
pendentes, com proxima dose em agora + novo intervalo; o historico consumido permanece.
Mudanca apenas de nome conserva os horarios da agenda.

## Roteiro rapido para apresentar

1. Execute o console e use a opcao 2 para mostrar dados que vieram do banco.
2. Cadastre um medicamento de teste com estoque 12, intervalo 12 horas e 1 unidade/dose.
   A autonomia sera de 6 dias.
3. Na opcao 6, escolha o ID da primeira dose desse medicamento e confirme a tomada.
   O estoque cai para 11 e o modelo calcula 5 dias inteiros, exibindo o alerta.
4. Na opcao 3, altere o nome; na opcao 4, confira e ajuste o estoque.
5. Na opcao 7, autorize a visualizacao local pelo cuidador e mostre pendencias e historico.
6. Saia e abra de novo: a informacao permanece no banco.
7. Exclua somente o medicamento de teste pela opcao 5.
8. Execute o script de testes para demonstrar rejeicao de duplicidade, falta de estoque,
   rollback e concorrencia.

O alerta e exibido no console e no painel autorizado. Nao ha envio de SMS,
WhatsApp ou notificacao externa. O consentimento da opcao 7 e local e vale para a
visualizacao solicitada; nao equivale a autenticacao de usuarios ou autorizacao
persistida. Os lembretes sao atualizados ao retornar ao menu, sem servico em segundo plano.

## Como colocar no GitHub com sua contribuicao

### Pelo navegador (Upload files)

1. Baixe e extraia este ZIP.
2. Abra o repositorio correto no GitHub, na branch de trabalho combinada com o grupo.
3. Em **Add file > Upload files**, arraste **o conteudo extraido**:
   `src`, `scripts`, `tests`, `docs` e este README, mantendo os caminhos.
   Nao envie apenas o ZIP nem crie uma pasta externa envolvendo esses arquivos.
4. Confira que somente os tres arquivos Java de producao listados no inicio serao
   substituidos e os demais arquivos deste pacote adicionados.
5. Use sua conta e a mensagem:
   `feat(view): criacao do menu interativo CLI e integracao do fluxo de confirmacao de doses`
6. Apos o upload, confira os caminhos no repositorio. Se o grupo ja alterou o
   `Main.java`, revise a integracao em uma branch antes de substituir esse arquivo.

### Pelo Git local

No clone do repositorio, depois de copiar os arquivos e rodar os testes:

```powershell
git config user.name "Carlos Henrique"
git config user.email "COLOQUE_AQUI_SEU_EMAIL_VERIFICADO_DO_GITHUB"
git status
git diff -- src/Main.java src/dao/DoseDAO.java src/views/ConsoleView.java
git add src/Main.java src/dao/DoseDAO.java src/views/ConsoleView.java
git add README_CARLOS_HENRIQUE.md docs/entrega-2-carlos-henrique.md
git add scripts/executar-carlos.ps1 scripts/executar-carlos.sh scripts/testar-carlos.ps1
git add tests/CarlosHenriqueIntegrationTest.java
git commit -m "feat(view): criacao do menu interativo CLI e integracao do fluxo de confirmacao de doses"
git push
```

Substitua o email de exemplo pelo email verificado da sua conta (ou o noreply
fornecido pelo GitHub). Nao use o email do Lucas. O pacote nao configura o Git,
nao cria commits em nome de terceiros e nao envia nada ao repositorio automaticamente.
A segunda entrega do grupo deve reunir tambem a contribuicao do Lucas; este ZIP
representa somente a sua parte.
