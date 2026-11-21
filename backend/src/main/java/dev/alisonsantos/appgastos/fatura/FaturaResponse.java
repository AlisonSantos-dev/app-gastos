package dev.alisonsantos.appgastos.fatura;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma fatura com a conciliação: o total oficial do banco comparado
 * com a soma dos gastos que o app tem dessa fatura.
 *
 * @param bate true quando a diferença é de até R$ 1,00 (tolerância para
 *             os centavos de arredondamento de parcelas)
 */
public record FaturaResponse(
        Long id,
        LocalDate fechamento,
        LocalDate vencimento,
        String status,
        BigDecimal totalOficial,
        BigDecimal somaGastos,
        BigDecimal diferenca,
        long quantidadeLancamentos,
        boolean bate
) {
}
