package dev.alisonsantos.appgastos.fechamento;

import dev.alisonsantos.appgastos.pessoa.Pessoa;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma linha do fechamento: a parte de UMA pessoa em UMA compra ou conta.
 * Uma compra dividida entre 5 pessoas vira 5 linhas.
 *
 * @param pessoa     null = sem dono
 * @param valor      a parte desta pessoa
 * @param valorTotal o valor inteiro da compra ou da conta
 */
public record LinhaFechamento(
        Pessoa pessoa,
        OrigemItem origem,
        Long transacaoId,
        Long contaFixaMesId,
        LocalDate data,
        String descricao,
        String detalhe,
        BigDecimal valor,
        BigDecimal valorTotal,
        boolean dividida
) {
}
