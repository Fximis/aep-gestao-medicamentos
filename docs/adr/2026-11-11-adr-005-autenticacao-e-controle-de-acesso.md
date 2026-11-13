# ADR-005 — Autenticação e controle de acesso familiar

**Status** Vigente  
**Data** 2026-11-11

## Contexto

A versão integrada selecionava automaticamente um paciente de demonstração na interface gráfica. O console permitia escolher um paciente existente, e o modelo comparava senhas armazenadas em texto puro. O acompanhamento familiar dependia de uma confirmação local durante a sessão, sem vínculo persistido entre contas.

A segunda entrega precisa identificar quem está usando o aplicativo e aplicar a RN08. Cada paciente deve acessar seu próprio tratamento e poder autorizar ou revogar a consulta por um responsável cadastrado.

## Decisões

| Mudança | Motivo |
| --- | --- |
| Abrir a tela de acesso antes do painel. Oferecer cadastro, entrada, troca de senha e encerramento de sessão em Swing e no console. | Vincular a navegação a uma identidade autenticada. |
| Centralizar a autenticação em `auth.Autenticacao` e a derivação de senha em `auth.Senhas`. Manter identidade e dados de perfil privados nos modelos. | Separar credenciais das entidades e aplicar as mesmas regras nos dois modos. |
| Armazenar PBKDF2-HMAC-SHA256 com 600.000 iterações, salt aleatório de 16 bytes por credencial e chave derivada de 32 bytes. Comparar os resultados com `MessageDigest.isEqual`. | Aumentar o custo de adivinhar senhas a partir do banco e diferenciar credenciais de contas que escolham a mesma senha. |
| Usar `JPasswordField` na GUI e `Console.readPassword` em terminal interativo. Limpar os vetores temporários e exibir uma mensagem genérica para e-mail inexistente ou senha incorreta. | Reduzir a exposição da senha na tela, na memória e nas respostas de login. |
| Validar nome, formato de e-mail, perfil e confirmação de senha. Exigir 5 a 128 caracteres para senhas novas. Normalizar e-mail e impedir duplicidade no cadastro. | Evitar registros inválidos e contas distintas que difiram apenas em maiúsculas ou espaços externos. |
| Aguardar 30 segundos após cinco tentativas de autenticação sem sucesso na execução atual. | Desacelerar tentativas repetidas pelos formulários. |
| Registrar autorizações em `acesso_familiar`, com chaves estrangeiras e vínculo único por paciente e responsável. Conceder ou revogar pelo perfil do próprio paciente. | Aplicar consentimento explícito e persistente conforme a RN08. |
| Restringir o responsável a consultas com vínculo ativo. Filtrar consultas e alterações do paciente por seu identificador, incluindo confirmações individuais e em lote. | Isolar os tratamentos e preservar o controle do paciente sobre estoque e doses. |
| Migrar credenciais existentes na inicialização e remover a criação automática das contas de demonstração. | Proteger os dados já cadastrados e eliminar a entrada com senhas de exemplo conhecidas. |

## Alterações por camada

- `src/auth/` contém política de senha, hash e sessão. O identificador da conta autenticada vem do registro persistido.
- `Usuario`, `Paciente` e `Responsavel` mantêm a hierarquia de POO e transportam apenas dados de perfil.
- `UsuarioDAO` mantém a leitura e a atualização do hash restritas ao cadastro, ao login, à migração e à troca de senha. As consultas com entradas do usuário usam parâmetros JDBC.
- `FamiliarDAO` persiste o consentimento. `MedicamentoDAO` e `DoseDAO` verificam propriedade e vínculo nas consultas dos fluxos autenticados.
- `LoginWindow` apresenta os formulários de acesso. `ResponsavelWindow` apresenta o acompanhamento em modo de leitura. `MainWindow` carrega a conta do paciente autenticado. `ConsoleView` oferece as mesmas opções de conta e consentimento em terminal.
- `DatabaseConnection` habilita chaves estrangeiras no SQLite, seleciona o banco uma vez por execução e executa a migração das credenciais. `database/schema.sql` inclui a tabela de consentimento no MySQL.
- A documentação Markdown recebeu títulos e descrições sem emojis. Os índices de documentação e de ADRs apontam para este registro.

Este ADR complementa o ADR-002 e atualiza a abertura do aplicativo descrita nos ADRs 001, 003 e 004. Os fluxos de estoque, autonomia e confirmação transacional de doses continuam disponíveis para o paciente autenticado.

## Migração e operação

Na primeira conexão, a aplicação converte credenciais legadas para o formato de hash dentro de uma transação. A migração preserva os identificadores, medicamentos e doses. As contas da carga de demonstração que ainda usam a senha pública original e os registros com senha em branco são desativados. Em uma base nova, o usuário começa pelo cadastro.

O e-mail é um identificador de conta local. A troca de senha exige a senha atual. O limite de tentativas vale durante a execução do programa. A proteção do arquivo SQLite e dos backups depende das permissões do sistema operacional. Backups criados antes da atualização podem conter as credenciais antigas.

O cadastro de um responsável, por si só, dá acesso ao menu de consulta. O paciente concede acesso informando o e-mail dessa conta. A revogação passa a valer nas consultas seguintes. A visualização autorizada inclui estoque, autonomia, alertas, agenda e histórico.

## Validação

- Compilação da aplicação e dos testes com Java 21.
- 71 verificações de modelos e do contrato DAO.
- 42 verificações de integração com SQLite para o console, CRUD, estoque, transações e consentimento.
- 35 verificações de autenticação e privacidade com SQLite, incluindo migração, hashes distintos, troca de senha, bloqueio temporário, escopo de consultas e revogação.
- Exercício dos formulários Swing para cadastro, login correto e incorreto, autorização, consulta do responsável e logout.

A integração em servidor MySQL ficou fora da validação desta revisão.

O teste `tests/AutenticacaoIntegrationTest.java` usa um banco de teste criado em um diretório temporário vazio. Compile-o junto com as fontes ou com as classes da aplicação no classpath. Execute a classe `AutenticacaoIntegrationTest` desse diretório, incluindo os caminhos absolutos da pasta `bin/` e dos drivers `lib/*` no classpath.

## Referência

A escolha do fator de trabalho do PBKDF2 segue a orientação para PBKDF2-HMAC-SHA256 publicada pela OWASP. [Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
