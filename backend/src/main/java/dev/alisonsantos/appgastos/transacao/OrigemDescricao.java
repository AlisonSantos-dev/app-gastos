package dev.alisonsantos.appgastos.transacao;

/**
 * De onde veio a descrição amigável da transação.
 * A ordem de prioridade é: MANUAL > REGRA > BANCO.
 *
 * BANCO: ainda não foi identificada (mostra a descrição do banco).
 * REGRA: definida por uma regra da legenda.
 * MANUAL: editada à mão. Nunca é sobrescrita pelo sync nem por regras.
 */
public enum OrigemDescricao {
    BANCO,
    REGRA,
    MANUAL
}
