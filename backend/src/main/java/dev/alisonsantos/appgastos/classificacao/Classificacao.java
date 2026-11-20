package dev.alisonsantos.appgastos.classificacao;

import dev.alisonsantos.appgastos.transacao.TipoLancamento;

/**
 * O resultado da classificação de um lançamento.
 *
 * @param tipo           o que o lançamento é (compra, estorno, pagamento de fatura...)
 * @param contaNosGastos se entra nos totais de gastos
 * @param motivo         qual regra decidiu, em português (útil para investigar)
 */
public record Classificacao(TipoLancamento tipo, boolean contaNosGastos, String motivo) {
}
