# Cuidado Diário — Gestão de Medicamentos

Projeto da Atividade de Estudo Programada (AEP) do 4º semestre. A aplicação permite cadastrar medicamentos, acompanhar o estoque e os horários, confirmar doses e autorizar a consulta por responsáveis. A confirmação de uma dose reduz o estoque e atualiza a autonomia. O sistema sinaliza recompra quando restam cinco dias ou menos.

## 1. Pré-requisitos

- **JDK 21 ou superior.** Execute `java -version` e `javac -version` no terminal para confirmar que os dois comandos estão disponíveis.
- Área de trabalho gráfica para a interface Swing. O modo terminal também está disponível.
- Os drivers JDBC já estão na pasta `lib/`: `sqlite-jdbc-3.46.1.3.jar` e `mysql-connector-j-8.4.0.jar`. A compilação usa `javac` diretamente, sem Maven ou Gradle.

## 2. Compilar e iniciar com SQLite

Baixe ou clone o repositório. Se baixar o ZIP, extraia-o. Abra o terminal **na raiz do projeto**, onde estão este `README.md`, a pasta `src/` e os arquivos de inicialização.

**Windows (PowerShell)**

```powershell
$env:AEP_BANCO = "sqlite"
.\iniciar.bat
```

**Linux ou macOS**

```sh
AEP_BANCO=sqlite sh iniciar.sh
```

Os scripts compilam os arquivos Java para `bin/` e abrem a tela de entrada. O SQLite cria as tabelas em `database/gestao_medicamentos.db` na primeira conexão. Para esse modo, pode seguir direto ao cadastro da conta.

## 3. Primeiro acesso e demonstração

1. Na tela inicial, abra **Cadastro**. Preencha nome, e-mail e senha de 5 a 128 caracteres. Escolha o perfil **Paciente** e clique em **Criar conta**.
2. Entre com o e-mail e a senha cadastrados.
3. Em **Medicamentos**, clique em **Novo medicamento** e cadastre um item fictício com estoque, intervalo entre doses e quantidade por dose.
4. Em **Minha rotina**, selecione uma dose pendente e clique em **Confirmar dose selecionada**. Volte a **Medicamentos** para verificar o saldo e a autonomia atualizados. Para demonstrar o alerta, use um item fictício com estoque de 6 unidades, intervalo de 24 horas e 1 unidade por dose. Após a confirmação, a autonomia passa de 6 para 5 dias.
5. Para testar o acesso familiar, crie uma segunda conta com perfil **Responsável**. Na conta do paciente, vá a **Minha família → Gerenciar acessos**, informe o e-mail do responsável e autorize a consulta. O nome aparece na lista de responsáveis autorizados. Ao entrar como responsável, selecione o paciente e clique em **Consultar**.

## 4. Usar MySQL (opcional)

1. Inicie o MySQL local na porta **3306**.
2. Na raiz do projeto, importe o esquema com um usuário administrador do banco. O script cria o banco `gestao_medicamentos` e suas tabelas.

   ```sh
   mysql -u root -p -e "source database/schema.sql"
   ```

3. Configure o usuário e a senha **do banco** no mesmo terminal em que vai iniciar a aplicação. Use uma conta com permissões para consultar e alterar dados e criar as tabelas necessárias.

   **Windows (PowerShell)**

   ```powershell
   $env:AEP_BANCO = "mysql"
   $env:AEP_MYSQL_USER = "seu_usuario_mysql"
   $env:AEP_MYSQL_PASSWORD = "sua_senha_mysql"
   .\iniciar.bat
   ```

   **Linux ou macOS**

   ```sh
   export AEP_BANCO=mysql
   export AEP_MYSQL_USER='seu_usuario_mysql'
   export AEP_MYSQL_PASSWORD='sua_senha_mysql'
   sh iniciar.sh
   ```

4. Crie as contas **da aplicação** pela tela de cadastro. Essas contas são usadas para acessar o sistema como paciente ou responsável.

## 5. Compilar manualmente ou usar o terminal

Execute os comandos na raiz do projeto.

**Windows (PowerShell)**

```powershell
$env:AEP_BANCO = "sqlite"
$fontes = Get-ChildItem -Path src -Recurse -Filter *.java -File | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp "lib/*;src" -d bin $fontes
java -cp "bin;lib/*" Main
```

Para iniciar no terminal após compilar, execute `java -cp "bin;lib/*" Main --cli`.

**Linux ou macOS**

```sh
export AEP_BANCO=sqlite
javac -encoding UTF-8 -cp 'lib/*:src' -d bin src/auth/*.java src/config/*.java src/dao/*.java src/models/*.java src/views/*.java src/views/gui/*.java src/Main.java
java -cp 'bin:lib/*' Main
```

Para iniciar no terminal após compilar, execute `java -cp 'bin:lib/*' Main --cli`. O cadastro e o login nesse modo precisam de um terminal interativo.

## Problemas comuns

| Sintoma | O que conferir |
| --- | --- |
| `javac` não encontrado | Instale o JDK 21 ou superior, adicione a pasta `bin` do JDK ao `PATH` e abra um terminal novo. |
| Erro de versão do Java ao executar | Confira se `java` e `javac` apontam para o mesmo JDK. Compile novamente com o script de inicialização. |
| Driver JDBC ausente | Confira se os dois arquivos `.jar` estão em `lib/` e execute o comando na raiz do projeto. |
| Falha ao conectar ao MySQL | Confira se o servidor está ativo, se o esquema foi importado e se as credenciais do banco estão corretas. Para testar a aplicação com SQLite, use a seção 2. |

## Estrutura e documentação

| Caminho | Conteúdo |
| --- | --- |
| `src/` | Código Java da aplicação, incluindo autenticação, modelos, DAOs e interface. |
| `database/schema.sql` | Script de criação do banco MySQL. |
| `lib/` | Drivers JDBC usados em execução. |
| `tests/` | Testes dos modelos, da integração com o banco e da autenticação. |
| `docs/` | Documentação acadêmica e decisões de arquitetura. |

- [Documentação do banco de dados](database/README.md)
- [Decisões de arquitetura](docs/adr/README.md)
- [Autenticação e permissões familiares](docs/adr/2026-11-11-adr-005-autenticacao-e-controle-de-acesso.md)

## Integrantes

| Nome | R.A. |
| --- | --- |
| Fellipe Miquelão Schmidt | 25153218-2 |
| Lucas Thomaz Brandel Zequini | 25169037-2 |
| Carlos Henrique Saran Pappa | 25143592-2 |
