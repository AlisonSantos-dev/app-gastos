-- =====================================================================
-- V3: Origem da classificação da transação
--
-- Assim como a descrição e a pessoa, a classificação também guarda de
-- onde veio: das regras automáticas ou de uma correção manual.
-- A classificação MANUAL nunca é sobrescrita pelo sync.
-- =====================================================================

ALTER TABLE transacao
    ADD COLUMN origem_classificacao VARCHAR(20) NOT NULL DEFAULT 'AUTOMATICA';

COMMENT ON COLUMN transacao.origem_classificacao IS 'AUTOMATICA (regras) ou MANUAL (corrigida à mão, nunca sobrescrita)';

-- Índice para os totais por tipo (ex.: somar só as compras de uma fatura)
CREATE INDEX idx_transacao_tipo_lancamento ON transacao (tipo_lancamento);
