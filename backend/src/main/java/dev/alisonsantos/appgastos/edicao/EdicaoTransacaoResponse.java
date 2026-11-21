package dev.alisonsantos.appgastos.edicao;

import dev.alisonsantos.appgastos.transacao.TransacaoResponse;

import java.util.List;

/**
 * Resultado de uma edição: quantas transações mudaram (a editada e,
 * se for parcelada, as outras parcelas) e como elas ficaram.
 */
public record EdicaoTransacaoResponse(
        int quantidade,
        List<TransacaoResponse> transacoes
) {
}
