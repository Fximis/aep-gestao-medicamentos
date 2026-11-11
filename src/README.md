# Código-Fonte da Aplicação (Java / POO / JDBC)

Este diretório contém a implementação completa da **2ª Entrega da AEP (Software Funcional Integrado)**, estruturada segundo o padrão MVC (Model-View-Controller) e os princípios SOLID/POO.

---

## Estrutura de Pacotes

```text
src/
├── auth/                          # Cadastro, sessão e hash de senha
├── config/
│   └── DatabaseConnection.java    # Singleton/Factory de conexão JDBC ativa (MySQL / SQLite)
│
├── dao/
│   ├── GenericDAO.java            # Interface genérica de persistência (Pilar de Abstração)
│   ├── MedicamentoDAO.java        # CRUD completo da entidade principal (Create, Read, Update, Delete)
│   ├── DoseDAO.java               # Agendamento, baixa de estoque e transação ACID
│   ├── UsuarioDAO.java            # Persistência de perfis e credenciais
│   └── FamiliarDAO.java           # Consentimento e consulta autorizada
│
├── models/
│   ├── Usuario.java               # Classe Abstrata (Abstração / Encapsulamento)
│   ├── Paciente.java              # Subclasse herdeira com @Override de obterTipo() (Herança e Polimorfismo)
│   ├── Responsavel.java           # Subclasse herdeira com @Override de obterTipo() (Herança e Polimorfismo)
│   ├── Medicamento.java           # Regras matemáticas de autonomia e gatilho de recompra (<= 5 dias)
│   └── Dose.java                  # Horários de consumo, data/hora e confirmação ativa
│
├── views/
│   ├── ConsoleView.java           # Acesso e operações em terminal
│   └── gui/                       # Formulários de conta e painéis Swing
│
└── Main.java                      # Ponto de entrada (Bootstrap), orquestrador do loop e menus
```

---

## Como Compilar e Executar

Consulte o arquivo [`README.md`](../README.md) na raiz do projeto para os comandos passo a passo de compilação via terminal PowerShell ou CMD.
