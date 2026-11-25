package dev.alisonsantos.appgastos.recebimento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Um Pix recebido que parece ser o pagamento de alguém.
 * Confirmar (o "tique") = POST /api/recebimentos com os dados da sugestão.
 *
 * @param competenciaSugerida o mês mais antigo que a pessoa ainda deve
 *                            (ou o mês anterior ao Pix, se ela não deve nada)
 */
public record SugestaoRecebimentoResponse(
        Long transacaoId,
        LocalDate data,
        BigDecimal valor,
        String nomeNoExtrato,
        String conta,
        Long pessoaId,
        String pessoa,
        YearMonth competenciaSugerida
) {
}
