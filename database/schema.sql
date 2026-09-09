-- Criação do banco de dados (se aplicável ao SGBD, ex: MySQL ou PostgreSQL)
CREATE DATABASE IF NOT EXISTS gestao_medicamentos;
USE gestao_medicamentos;

-- Tabela de Usuários (Implementa herança via Single Table)
CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha VARCHAR(255) NOT NULL,
    tipo VARCHAR(20) NOT NULL, -- 'PACIENTE' ou 'RESPONSAVEL'
    condicao_medica VARCHAR(255), -- Específico para Paciente
    vinculo_familiar VARCHAR(100) -- Específico para Responsavel
);

-- Tabela de Medicamentos
CREATE TABLE medicamento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    quantidade_estoque INT NOT NULL DEFAULT 0,
    frequencia_horas INT NOT NULL,
    quantidade_por_dose INT NOT NULL DEFAULT 1,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

-- Tabela de Doses (Histórico e Agenda)
CREATE TABLE dose (
    id INT AUTO_INCREMENT PRIMARY KEY,
    medicamento_id INT NOT NULL,
    horario_previsto DATETIME NOT NULL,
    horario_consumo DATETIME,
    status_tomado BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (medicamento_id) REFERENCES medicamento(id) ON DELETE CASCADE
);
