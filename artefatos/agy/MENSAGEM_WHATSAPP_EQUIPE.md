# Mensagens para Envio no WhatsApp — Alinhamento e Delegação da Equipe AEP

> **Instruções para o Fellipe:**  
> Copie e cole os blocos abaixo diretamente no grupo de WhatsApp da AEP ou nas conversas individuais com o **Lucas** e com o **Carlos**.  
> O texto já está formatado com a sintaxe do WhatsApp (`*negrito*`, `_itálico_`, marcadores e emojis).

---

## 📱 BLOCO 1: Mensagem para o Grupo Geral da Equipe (Alinhamento & Alerta do Edital)

```text
Fala pessoal! 🚀
Finalizei o diagnóstico completo do edital da AEP da UniCesumar para a *2ª Entrega (Software Funcional Integrado)*.

A 1ª entrega (documentação) já está 100% aprovada e mergeada. Agora a 2ª entrega vale *1,0 ponto na média final de TODAS as matérias do semestre*.

⚠️ *ALERTA MÁXIMO DO EDITAL (Para não zerarmos):*
1. *Zero em Banco:* O professor deu aviso explícito no edital: se o sistema rodar com listas em memória (simulação de CRUD sem banco conectado via JDBC), a nota de persistência é *ZERO automático*.
2. *Pilares de POO obrigatórios:* Tem que ter Classe Abstrata/Interface, Herança e Polimorfismo com `@Override`. A ausência de um deles zera o critério de POO (0,3).
3. *CRUD Completo:* Create, Read, Update e Delete funcionando de verdade no banco.
4. *Contribuição de TODOS:* O edital exige que constem commits do *Fellipe*, do *Lucas* e do *Carlos* no repositório.

Já montei a arquitetura base em Java com JDBC puro e MySQL. Abaixo vou mandar a parte exata de cada um para fecharmos com nota 10 sem retrabalho! 👇
```

---

## 📱 BLOCO 2: Mensagem Individual para o LUCAS (Lucas Thomaz Brandel Zequini)

```text
Fala @Lucas! Tudo certo? 👊
Para a sua parte na 2ª Entrega da AEP, separei o foco no *Coração de POO e Regras de Negócio do Domínio*.

📌 *Sua Missão:*
1. *Camada de Modelos (POO):*
   - Refinar as classes de `src/models/` garantindo encapsulamento rígido (atributos `private`, getters/setters e construtores).
   - Validar a regra *RN04*: o estoque nunca pode ser negativo (bloquear no `Medicamento.java`).
   - Implementar o recálculo automático de autonomia (`calcularDiasRestantes`) e a verificação proativa de recompra (`verificarAlertaRecompra` <= 5 dias).
2. *Abstração / Interface:*
   - Criar e assinar a interface `src/dao/GenericDAO.java` com métodos padronizados (`inserir`, `buscarPorId`, `listarTodos`, `atualizar`, `excluir`).
3. *Commit no Git com o seu usuário:*
   - Para o professor ver a sua contribuição, configure seu Git local antes de commitar:
     `git config user.name "Lucas Thomaz"`
     `git config user.email "seu-email-do-github@exemplo.com"`
   - Mensagem de commit sugerida:
     `feat(models): implementacao dos pilares de POO, interface GenericDAO e regra de alerta de 5 dias`

Qualquer dúvida no código ou na regra me avisa que alinhamos!
```

---

## 📱 BLOCO 3: Mensagem Individual para o CARLOS (Carlos Henrique Saran Pappa)

```text
Fala @Carlos! Tudo certo? 👊
Para a sua parte na 2ª Entrega da AEP, separei o foco no *Ciclo de Doses e Interface do Usuário (CLI / Console)*.

📌 *Sua Missão:*
1. *Agendamento e Baixa de Doses (`DoseDAO.java`):*
   - Implementar a listagem das doses pendentes do paciente (`horario_previsto` e `status_tomado = false`).
   - Implementar a confirmação de tomada da dose (`RF03`), que marca como tomada no banco e deduz a quantidade do estoque (`RN01` e `RN06`).
2. *Menu Interativo do Console (`ConsoleView.java`):*
   - Montar o menu do terminal via `Scanner` para o professor conseguir testar fácil:
     [1] Cadastrar Medicamento (Create)
     [2] Listar Medicamentos e Autonomia (Read)
     [3] Atualizar Medicamento (Update)
     [4] Ajustar Estoque Manualmente (Update de Estoque)
     [5] Excluir Medicamento (Delete)
     [6] Confirmar Dose Tomada (Baixa + Alerta se <= 5 dias)
     [7] Visão do Responsável / Cuidador
     [0] Sair
3. *Commit no Git com o seu usuário:*
   - Configure seu Git local antes do commit:
     `git config user.name "Carlos Henrique"`
     `git config user.email "seu-email-do-github@exemplo.com"`
   - Mensagem de commit sugerida:
     `feat(view): criacao do menu interativo CLI e integracao do fluxo de confirmacao de doses`

Bora garantir esse 1,0 ponto! Tamo junto!
```

---

## 📱 BLOCO 4: Mensagem de Apoio Técnico (Como Rodar e Testar na Máquina Deles)

```text
🛠️ *Como rodar o projeto localmente para testar:*

1. *Pré-requisitos:*
   - Java JDK 21 (ou superior) instalado.
   - MySQL ativo (pode ser pelo Laragon ou MySQL Workbench).
   
2. *Criar o banco:*
   - Abra o MySQL e rode o script que está em `database/schema.sql`. Ele já cria a base `gestao_medicamentos` com as tabelas e dados de teste.

3. *Compilar e Executar no terminal (PowerShell / CMD):*
   ```powershell
   # Compilar:
   javac -cp "lib/*;src" -d bin src/models/*.java src/config/*.java src/dao/*.java src/views/*.java src/Main.java

   # Executar:
   java -cp "bin;lib/*" Main
   ```

O driver JDBC do MySQL já está na pasta `lib/` do repositório, não precisa instalar nada além do Java! 🚀
```
