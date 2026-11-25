package dev.alisonsantos.appgastos.fechamento;

/** De onde vem cada linha do fechamento. */
public enum OrigemItem {

    /** Uma compra da fatura do cartão que vence no mês. */
    CARTAO,

    /** Uma saída da conta corrente no mês (que não é pagamento de conta fixa). */
    CONTA_CORRENTE,

    /** Uma conta fixa que vence no mês. */
    CONTA_FIXA
}
