# Banco de dados da segunda entrega

A aplicação usa JDBC com os drivers MySQL e SQLite incluídos em `lib/`.

| Tabela | Conteúdo |
| --- | --- |
| `usuario` | Identidade, perfil e hash da senha. |
| `medicamento` | Estoque e posologia associados ao paciente. |
| `dose` | Agenda, confirmações e horário real de consumo. |
| `acesso_familiar` | Vínculos de leitura autorizados pelo paciente. |

## SQLite

A aplicação cria o arquivo `database/gestao_medicamentos.db` e as tabelas na primeira conexão. Crie as contas pelo formulário de cadastro. Para escolher esse modo explicitamente, defina `AEP_BANCO=sqlite` no ambiente.

## MySQL

Importe `database/schema.sql` no MySQL local. O esquema usa o banco `gestao_medicamentos` na porta 3306.

```sh
mysql -u root -p -e "source database/schema.sql"
```

Defina `AEP_BANCO=mysql`, `AEP_MYSQL_USER` e `AEP_MYSQL_PASSWORD` no ambiente de execução. O modo `auto`, usado por padrão, tenta MySQL e seleciona SQLite se a conexão inicial estiver indisponível. A seleção permanece durante a execução.

A inicialização migra credenciais antigas e prepara a tabela de acesso familiar. Consulte a decisão de migração e os limites de operação. [ADR-005](adr/2026-11-11-adr-005-autenticacao-e-controle-de-acesso.md)
