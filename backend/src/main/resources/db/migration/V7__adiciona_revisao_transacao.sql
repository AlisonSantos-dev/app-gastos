-- =====================================================================
-- V7: Revisão das transações (caixa de entrada)
--
-- Uma compra sai da caixa de entrada quando você a revisa: escolhe o
-- dono ou confirma que está tudo certo.
-- =====================================================================

ALTER TABLE transacao ADD COLUMN revisada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE transacao ADD COLUMN revisada_em TIMESTAMPTZ;

COMMENT ON COLUMN transacao.revisada IS 'Verdadeiro quando você já revisou a compra (ela sai da caixa de entrada)';
COMMENT ON COLUMN transacao.revisada_em IS 'Quando a compra foi revisada';

-- Índice parcial: só as transações que podem estar na caixa de entrada.
-- Fica pequeno, porque a maioria das transações já foi revisada.
CREATE INDEX idx_transacao_caixa ON transacao (data_transacao)
    WHERE revisada = FALSE AND conta_nos_gastos = TRUE;
