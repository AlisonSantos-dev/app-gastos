package dev.alisonsantos.appgastos.fechamento;

import java.math.BigDecimal;

/**
 * O total de uma pessoa no mês, separado por origem.
 * pessoaId null = o grupo "sem dono".
 */
public record TotalPessoa(
        Long pessoaId,
        String pessoa,
        String cor,
        BigDecimal total,
        BigDecimal cartoes,
        BigDecimal contaCorrente,
        BigDecimal contasFixas,
        int itens
) {
}
