package dev.alisonsantos.appgastos.divisao;

import dev.alisonsantos.appgastos.transacao.TransacaoResponse;

import java.util.List;

/**
 * Resultado da divisão em lote.
 *
 * @param quantidade quantas transações foram divididas (inclui outras parcelas)
 * @param avisos     as que não puderam ser divididas, e o motivo
 */
public record DivisaoLoteResponse(int quantidade, List<String> avisos, List<TransacaoResponse> transacoes) {
}
