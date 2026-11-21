-- =====================================================================
-- V4: Pessoas candidatas de um cartão compartilhado
--
-- Quando o cartão está no modo PERGUNTAR (ex.: o virtual usado no Uber
-- de duas pessoas), estas são as pessoas que podem ter feito a compra.
-- Na caixa de entrada, elas aparecem como botões rápidos.
-- =====================================================================

CREATE TABLE cartao_candidato (
    cartao_id  BIGINT NOT NULL,
    pessoa_id  BIGINT NOT NULL,

    CONSTRAINT pk_cartao_candidato PRIMARY KEY (cartao_id, pessoa_id),
    CONSTRAINT fk_cartao_candidato_cartao FOREIGN KEY (cartao_id) REFERENCES cartao (id) ON DELETE CASCADE,
    CONSTRAINT fk_cartao_candidato_pessoa FOREIGN KEY (pessoa_id) REFERENCES pessoa (id)
);
