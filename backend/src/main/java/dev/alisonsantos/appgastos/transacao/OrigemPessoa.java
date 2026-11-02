package dev.alisonsantos.appgastos.transacao;

/**
 * De onde veio o destino (a pessoa) da transação.
 * A ordem de prioridade é: MANUAL > REGRA > CARTAO / CONTA.
 *
 * CARTAO: dono padrão do cartão usado.
 * CONTA: dono padrão da conta (lançamentos sem cartão, como débito).
 * REGRA: definida por uma regra.
 * MANUAL: escolhida à mão. Nunca é sobrescrita automaticamente.
 */
public enum OrigemPessoa {
    CARTAO,
    CONTA,
    REGRA,
    MANUAL
}
