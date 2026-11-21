-- =====================================================================
-- V6: Categoria editada à mão
--
-- Assim como a descrição, a pessoa e a classificação, a categoria
-- também pode ser corrigida à mão. Quando isso acontece, a legenda
-- não sobrescreve mais.
-- =====================================================================

ALTER TABLE transacao
    ADD COLUMN categoria_manual BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN transacao.categoria_manual IS 'Verdadeiro quando a categoria foi definida à mão (a legenda não sobrescreve)';

-- Índice para achar as parcelas da mesma compra rapidamente
CREATE INDEX idx_transacao_compra ON transacao (cartao_id, data_compra, parcela_total);
