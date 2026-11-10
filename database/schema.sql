-- ====================================================================
-- SISTEMA DE GESTÃO DE MEDICAMENTOS CONTÍNUOS E ESTOQUE FAMILIAR (AEP)
-- Script DDL de Estrutura Relacional e Dados Iniciais (Seed)
-- Compatível com MySQL 8.x / MariaDB e SQLite
-- ====================================================================

CREATE DATABASE IF NOT EXISTS gestao_medicamentos;
USE gestao_medicamentos;

-- 1. Tabela de Usuários (Implementa herança via Single Table Pattern)
CREATE TABLE IF NOT EXISTS usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    tipo VARCHAR(20) NOT NULL,          -- 'PACIENTE' ou 'RESPONSAVEL'
    condicao_medica VARCHAR(255),       -- Atributo específico de Paciente
    vinculo_familiar VARCHAR(100)       -- Atributo específico de Responsável
);

-- 2. Tabela de Medicamentos (Entidade Principal do CRUD)
CREATE TABLE IF NOT EXISTS medicamento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    quantidade_estoque INT NOT NULL DEFAULT 0,
    frequencia_horas INT NOT NULL,
    quantidade_por_dose INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_medicamento_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

-- 3. Tabela de Doses (Histórico e Agenda de Notificação)
CREATE TABLE IF NOT EXISTS dose (
    id INT AUTO_INCREMENT PRIMARY KEY,
    medicamento_id INT NOT NULL,
    horario_previsto DATETIME NOT NULL,
    horario_consumo DATETIME,
    status_tomado BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dose_medicamento FOREIGN KEY (medicamento_id) REFERENCES medicamento(id) ON DELETE CASCADE
);

-- ====================================================================
-- CARGA INICIAL PARA TESTES E DEMONSTRAÇÃO (SEED)
-- ====================================================================

-- Inserção de Paciente e Responsável Familiar (Herança / Polimorfismo)
INSERT INTO usuario (id, nome, email, senha, tipo, condicao_medica, vinculo_familiar) VALUES
(1, 'João da Silva', 'joao@email.com', '123456', 'PACIENTE', 'Hipertensão e Diabetes Tipo 2', NULL),
(2, 'Maria Silva Santos', 'maria@email.com', '123456', 'RESPONSAVEL', NULL, 'Filha e Cuidadora');

-- Inserção de Medicamentos (Exemplos: um com alerta de compra e outro normal)
-- Losartana: 10 comp / (24/12 * 1) = 10 / 2 = 5 dias restantes (🚨 GATILHO DE RECOMPRA ATIVO)
-- Metformina: 60 comp / (24/8 * 1) = 60 / 3 = 20 dias restantes (Nível Seguro)
INSERT INTO medicamento (id, usuario_id, nome, quantidade_estoque, frequencia_horas, quantidade_por_dose) VALUES
(101, 1, 'Losartana Potássica 50mg', 10, 12, 1),
(102, 1, 'Cloridrato de Metformina 850mg', 60, 8, 1);

-- Inserção de Doses Programadas para o Paciente João
INSERT INTO dose (id, medicamento_id, horario_previsto, horario_consumo, status_tomado) VALUES
(1001, 101, DATE_ADD(NOW(), INTERVAL -2 HOUR), NULL, FALSE),
(1002, 101, DATE_ADD(NOW(), INTERVAL 10 HOUR), NULL, FALSE),
(1003, 102, DATE_ADD(NOW(), INTERVAL -1 HOUR), NULL, FALSE),
(1004, 102, DATE_ADD(NOW(), INTERVAL 7 HOUR), NULL, FALSE);
