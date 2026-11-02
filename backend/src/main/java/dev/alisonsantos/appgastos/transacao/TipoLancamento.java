package dev.alisonsantos.appgastos.transacao;

/**
 * O que o lançamento representa. Definido pela classificação (Etapa 8).
 *
 * COMPRA: um gasto de verdade.
 * ESTORNO: devolução de uma compra (abate do total).
 * PAGAMENTO_FATURA: pagamento de fatura de cartão. Não é gasto.
 * TARIFA: anuidade e outras tarifas.
 * TRANSFERENCIA_PROPRIA: entre contas do próprio titular. Não é gasto.
 * RECEBIMENTO: dinheiro recebido de outra pessoa (ex.: Pix de reembolso).
 * OUTRO: qualquer outra coisa.
 * NAO_CLASSIFICADO: ainda não passou pela classificação.
 */
public enum TipoLancamento {
    COMPRA,
    ESTORNO,
    PAGAMENTO_FATURA,
    TARIFA,
    TRANSFERENCIA_PROPRIA,
    RECEBIMENTO,
    OUTRO,
    NAO_CLASSIFICADO
}
