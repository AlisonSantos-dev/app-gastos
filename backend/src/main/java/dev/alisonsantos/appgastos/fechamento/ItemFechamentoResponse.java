package dev.alisonsantos.appgastos.fechamento;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma linha do detalhe de uma pessoa no mês. */
public record ItemFechamentoResponse(
        String origem,
        LocalDate data,
        String descricao,
        String detalhe,
        BigDecimal valor,
        BigDecimal valorTotal,
        boolean dividida,
        Long transacaoId,
        Long contaFixaMesId
) {

    static ItemFechamentoResponse de(LinhaFechamento l) {
        return new ItemFechamentoResponse(l.origem().name(), l.data(), l.descricao(), l.detalhe(),
                l.valor(), l.valorTotal(), l.dividida(), l.transacaoId(), l.contaFixaMesId());
    }
}
