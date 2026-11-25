package dev.alisonsantos.appgastos.contafixa;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Uma conta fixa como a API devolve.
 */
public record ContaFixaResponse(
        Long id,
        String nome,
        String grupo,
        Long contaId,
        String conta,
        int diaVencimento,
        BigDecimal valorEsperado,
        boolean variavel,
        Long pessoaId,
        String pessoa,
        Long divisaoSalvaId,
        String divisaoSalva,
        String chavePagamento,
        String categoria,
        YearMonth inicio,
        YearMonth fim,
        boolean ativa
) {

    public static ContaFixaResponse de(ContaFixa c) {
        return new ContaFixaResponse(
                c.getId(),
                c.getNome(),
                c.getGrupo(),
                c.getConta() == null ? null : c.getConta().getId(),
                c.getConta() == null ? null : c.getConta().getNome(),
                c.getDiaVencimento(),
                c.getValorEsperado(),
                c.getValorEsperado() == null,
                c.getPessoa() == null ? null : c.getPessoa().getId(),
                c.getPessoa() == null ? null : c.getPessoa().getNome(),
                c.getDivisaoSalva() == null ? null : c.getDivisaoSalva().getId(),
                c.getDivisaoSalva() == null ? null : c.getDivisaoSalva().getNome(),
                c.getChavePagamento(),
                c.getCategoria(),
                c.getInicio(),
                c.getFim(),
                c.isAtiva());
    }
}
