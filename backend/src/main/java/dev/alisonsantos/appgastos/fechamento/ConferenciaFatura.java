package dev.alisonsantos.appgastos.fechamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma fatura do mês: o total do banco contra a soma das compras no app. */
public record ConferenciaFatura(
        Long faturaId,
        String cartao,
        LocalDate vencimento,
        BigDecimal totalOficial,
        BigDecimal somaApp,
        BigDecimal diferenca,
        boolean bate
) {
}
