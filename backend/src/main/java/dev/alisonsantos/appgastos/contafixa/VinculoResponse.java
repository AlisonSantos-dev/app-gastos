package dev.alisonsantos.appgastos.contafixa;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Um pagamento encontrado: qual conta do mês, e qual transação do extrato. */
public record VinculoResponse(
        Long contaDoMesId,
        String conta,
        Long transacaoId,
        String descricaoBanco,
        BigDecimal valorPago,
        LocalDate dataPagamento
) {
}
