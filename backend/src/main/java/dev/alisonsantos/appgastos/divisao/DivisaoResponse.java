package dev.alisonsantos.appgastos.divisao;

import dev.alisonsantos.appgastos.transacao.TransacaoResponse;

import java.util.List;

/**
 * Resultado de uma divisão (ou de desfazer uma): quantas transações
 * mudaram (a escolhida e as outras parcelas) e como ficaram.
 */
public record DivisaoResponse(int quantidade, List<TransacaoResponse> transacoes) {
}
