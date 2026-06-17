-- Criação da Tabela de Contas (Atualizada com a coluna email)
CREATE TABLE conta (
    id VARCHAR(36) PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    saldo NUMERIC(18, 2) NOT NULL CONSTRAINT chk_saldo_positivo CHECK (saldo >= 0)
);

-- Criação da Tabela de Movimentações (Histórico/Extrato)
CREATE TABLE movimentacao (
    id VARCHAR(36) PRIMARY KEY,
    conta_origem_id VARCHAR(36) NOT NULL,
    conta_destino_id VARCHAR(36) NOT NULL,
    valor NUMERIC(18, 2) NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    CONSTRAINT fk_movimentacao_origem FOREIGN KEY (conta_origem_id) REFERENCES conta(id),
    CONSTRAINT fk_movimentacao_destino FOREIGN KEY (conta_destino_id) REFERENCES conta(id)
);

-- Criação da Tabela do Outbox (Padrão Transactional Outbox)
CREATE TABLE outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,    -- ex: 'TRANSFERENCIA'
    aggregate_id VARCHAR(36) NOT NULL,      -- id da movimentacao
    payload TEXT NOT NULL,                  -- JSON com os dados da notificação (incluirá o e-mail)
    status VARCHAR(20) NOT NULL,            -- 'PENDING', 'PROCESSED', 'FAILED'
    created_at TIMESTAMP NOT NULL
);

-- Criação de Índices para Performance do Scheduler do Outbox e Relatórios
CREATE INDEX idx_outbox_status_date ON outbox_events(status, created_at ASC);
CREATE INDEX idx_movimentacao_data ON movimentacao(data_criacao DESC);

-- Pré-carregamento de Clientes/Contas (Requisito 2.A)
-- Carga inicial de massa de dados: 10 artistas de cinema atuais
INSERT INTO conta (id, nome, email, saldo) VALUES
('CONTA-001', 'Pedro Pascal', 'pedro.pascal@cinema.com', 50000.00),
('CONTA-002', 'Zendaya', 'zendaya@cinema.com', 75000.00),
('CONTA-003', 'Timothee Chalamet', 'timothee.chalamet@cinema.com', 45000.00),
('CONTA-004', 'Florence Pugh', 'florence.pugh@cinema.com', 60000.00),
('CONTA-005', 'Austin Butler', 'austin.butler@cinema.com', 35000.00),
('CONTA-006', 'Margot Robbie', 'margot.robbie@cinema.com', 90000.00),
('CONTA-007', 'Ryan Gosling', 'ryan.gosling@cinema.com', 85000.00),
('CONTA-008', 'Cillian Murphy', 'cillian.murphy@cinema.com', 120000.00),
('CONTA-009', 'Ana de Armas', 'ana.armas@cinema.com', 55000.00),
-- Conta 010 criada estrategicamente com saldo zero para testes de falha
('CONTA-010', 'Tom Holland', 'tom.holland@cinema.com', 0.00);