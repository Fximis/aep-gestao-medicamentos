-- ====================================================================
-- SISTEMA DE GESTÃO DE MEDICAMENTOS CONTÍNUOS E ESTOQUE FAMILIAR (AEP)
-- Script DDL de Estrutura Relacional
-- MySQL 8.x / MariaDB. O SQLite é inicializado pela aplicação.
-- ====================================================================

CREATE DATABASE IF NOT EXISTS gestao_medicamentos;
USE gestao_medicamentos;

-- 1. Tabela de Usuários (Implementa herança via Single Table Pattern)
CREATE TABLE IF NOT EXISTS usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,         -- Hash PBKDF2 da credencial
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

-- 4. Autorizações explícitas de acompanhamento familiar
CREATE TABLE IF NOT EXISTS acesso_familiar (
    paciente_id INT NOT NULL,
    responsavel_id INT NOT NULL,
    PRIMARY KEY (paciente_id, responsavel_id),
    FOREIGN KEY (paciente_id) REFERENCES usuario(id) ON DELETE CASCADE,
    FOREIGN KEY (responsavel_id) REFERENCES usuario(id) ON DELETE CASCADE
);
