package dev.alisonsantos.appgastos.recebimento;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Quem já pagou o mês.
 *
 * @param publicado se o mês já foi publicado (sem isso, não há "devido")
 * @param aReceber  a soma dos saldos positivos (o que ainda falta receber)
 */
public record PainelRecebimentosResponse(
        YearMonth competencia,
        boolean publicado,
        BigDecimal totalDevido,
        BigDecimal totalRecebido,
        BigDecimal aReceber,
        List<SaldoPessoaResponse> pessoas
) {
}
