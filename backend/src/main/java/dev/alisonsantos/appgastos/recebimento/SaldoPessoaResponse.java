package dev.alisonsantos.appgastos.recebimento;

import java.math.BigDecimal;
import java.util.List;

/**
 * A situação de uma pessoa num mês.
 *
 * @param devido   o total dela no fechamento publicado (null se o mês não foi publicado)
 * @param recebido o que ela já pagou deste mês
 * @param saldo    devido - recebido (negativo = pagou a mais)
 */
public record SaldoPessoaResponse(
        Long pessoaId,
        String pessoa,
        String cor,
        BigDecimal devido,
        BigDecimal recebido,
        BigDecimal saldo,
        String situacao,
        List<RecebimentoResponse> recebimentos
) {
}
