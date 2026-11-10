# Diagnóstico do Ambiente Local e Remoto — AEP 4º Semestre

**Data do Levantamento:** 10/11/2026  
**Repositório:** `https://github.com/Fximis/aep-gestao-medicamentos.git`  
**Workspace Local:** `C:\Users\fximi\Desktop\Pasta God\04_Faculdade\80_AEP\4SBim1`  
**Modo de Inspeção:** Read-Only (Nenhum arquivo ou ponteiro de branch foi modificado).

---

## 1. Mapeamento do Repositório Remoto (GitHub)

### 1.1. Branches Remotas (`origin`)
O repositório no GitHub possui exatamente **2 branches** registradas:
1. `origin/master` (Branch padrão / HEAD)
2. `origin/docs/entrega-1-documentacao`

### 1.2. Histórico de Pull Requests
* **Pull Request #1:** `docs/entrega-1-documentacao` -> `master` (Merge realizado via commit `552831f`).
  - Conteúdo incorporado: Documentação formal da 1ª entrega, reformulação do README, identificação institucional dos integrantes da equipe.
* **Último Commit em `origin/master`:**
  - Hash: `418e721`
  - Mensagem: *"Simplify dynamic autonomy calculation section"*
  - Estado: Atualizado e em sincronia perfeita com a `master` local.

---

## 2. Mapeamento do Ambiente Local

### 2.1. Status do Working Tree (`master`)
* **Branch Ativa:** `master`
* **Status Git:** `Your branch is up to date with 'origin/master'.`
* **Arquivos Não Rastreados (Untracked Files):**
  - `1a_entrega.pdf` (189.465 bytes) — Arquivo PDF compilado da 1ª Entrega, presente na raiz local mas não commitado no Git.
* **Modificações não commitadas:** Nenhuma (`working tree clean`, exceto pelo untracked `1a_entrega.pdf`).

### 2.2. Branches Locais Existentes
1. `master` (alinhada a `origin/master`, commit `418e721`).
2. `docs/entrega-1-documentacao` (alinhada a `origin/docs/entrega-1-documentacao`).
3. **`feature/entrega-2` (Branch Crítica — Existe APENAS localmente):**
   - Esta branch **não foi enviada para o remoto** (`origin/feature/entrega-2` não existe).
   - Aponta para o commit `b2c8874` (*"fix: correcao de sintaxe no diagrama mermaid para evitar erro de parse"*).
   - Contém o commit fundamental `8865e51`: *"feat: entregavel 2. Script SQL, estruturacao de pacotes POO e logica do alerta de 5 dias em Java"*.

---

## 3. Inventário Detalhado dos Arquivos

### 3.1. Arquivos Presentes na Branch `master`
```text
C:\Users\fximi\Desktop\Pasta God\04_Faculdade\80_AEP\4SBim1/
├── 1a_entrega.pdf                                     [Untracked localmente]
├── Diretrizes Gerais da AEP (4º Semestre de 2026).pdf  [Edital e Rubrica Oficial UniCesumar]
├── README.md                                          [Documentação principal e espelho da 1ª entrega]
├── database/
│   └── README.md                                      [Placeholder de scripts SQL]
├── docs/
│   ├── 1a_entrega.md                                  [Documento formal em Markdown da 1ª entrega]
│   └── README.md                                      [Índice descritivo de documentação]
└── src/
    └── README.md                                      [Placeholder de código-fonte]
```
> **Observação:** Na branch `master`, ainda **não existem** classes Java compiláveis (`.java`) nem scripts SQL (`schema.sql`). Apenas os placeholders descritivos constam na árvore principal.

### 3.2. Arquivos Existentes no Rascunho da `feature/entrega-2`
Na branch local `feature/entrega-2`, foi iniciado o protótipo da 2ª Entrega:
```text
feature/entrega-2/
├── database/
│   └── schema.sql          [Script DDL com tabelas: usuario, medicamento, dose]
└── src/
    ├── Main.java           [Classe principal com simulação estática em memória]
    └── models/
        ├── Usuario.java     [Classe abstrata: id, nome, email, senha, obterTipo()]
        ├── Paciente.java    [Subclasse: herda Usuario, implementa obterTipo(), lista medicamentos]
        ├── Responsavel.java [Subclasse: herda Usuario, implementa obterTipo(), monitorarEstoque()]
        ├── Medicamento.java [Entidade: estoque, dose, cálculo dias restantes, alerta 5 dias]
        └── Dose.java        [Entidade: data/hora, status, marcarComoTomada()]
```

---

## 4. Comparativo de Divergência e Achados Chave

| Elemento | `origin/master` (Remoto) | `master` (Local) | `feature/entrega-2` (Local) | Situação / Alerta |
| :--- | :--- | :--- | :--- | :--- |
| **Documentação 1ª Entrega** | Completa (`1a_entrega.md`) | Completa + PDF gerado | Presente em commits antigos | Concluída e aprovada. |
| **Código Java (`src/models`)** | Inexistente (Apenas README) | Inexistente (Apenas README) | **Existe (5 classes de domínio)** | Rascunho inicial está apenas na branch local. |
| **Script SQL (`database/`)** | Inexistente (Apenas README) | Inexistente (Apenas README) | **Existe (`schema.sql`)** | DDL MySQL modelado, mas não está na `master`. |
| **Persistência / Conexão BD** | Inexistente | Inexistente | **Inexistente (Usa listas em memória)** | ⚠️ **Risco crítico de nota zero na persistência!** |
| **Operações CRUD** | Inexistente | Inexistente | **Incompleto (Apenas Create/Read em memória)** | ⚠️ Falta Update e Delete, e camada DAO. |
| **Sincronização Remota** | Sincronizado | Sincronizado | **Não enviada para o GitHub** | Colegas de equipe não têm acesso ao código. |

---

## 5. Conclusão do Diagnóstico
O projeto cumpriu integralmente os requisitos formais da **1ª Entrega** (que se encontra mergeada na `master` e refletida no GitHub). 

Para a **2ª Entrega**, há um ponto de partida valioso na branch local `feature/entrega-2` com a modelagem básica de POO e o DDL do banco. Contudo, esse código ainda é um protótipo em memória: **não possui conexão ativa com banco de dados (JDBC)**, **não implementa CRUD completo** e **não foi publicado no repositório remoto**.
