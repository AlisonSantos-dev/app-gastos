package dev.alisonsantos.appgastos.classificacao;

import dev.alisonsantos.appgastos.transacao.TipoLancamento;

import java.util.Map;

/**
 * Resultado da reclassificação de todas as transações.
 *
 * @param total               quantas transações foram analisadas
 * @param alteradas           quantas mudaram de classificação
 * @param manuaisPreservadas  quantas foram puladas por terem sido corrigidas à mão
 * @param porTipo             quantas ficaram em cada tipo
 */
public record ResumoClassificacao(
        int total,
        int alteradas,
        int manuaisPreservadas,
        Map<TipoLancamento, Long> porTipo
) {
}
