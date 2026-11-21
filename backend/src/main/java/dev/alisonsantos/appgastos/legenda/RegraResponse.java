package dev.alisonsantos.appgastos.legenda;

import java.time.LocalDate;

/**
 * Uma regra como a API devolve, com quantas transações ela identificou.
 */
public record RegraResponse(
        Long id,
        String tipoChave,
        String valorChave,
        String cartaoFinal,
        String descricao,
        String categoria,
        Long pessoaId,
        String pessoa,
        int prioridade,
        LocalDate vigenciaInicio,
        LocalDate vigenciaFim,
        boolean ativa,
        long usos
) {

    public static RegraResponse de(Regra regra, long usos) {
        return new RegraResponse(
                regra.getId(),
                regra.getTipoChave().name(),
                regra.getValorChave(),
                regra.getCartaoFinal(),
                regra.getDescricao(),
                regra.getCategoria(),
                regra.getPessoa() == null ? null : regra.getPessoa().getId(),
                regra.getPessoa() == null ? null : regra.getPessoa().getNome(),
                regra.getPrioridade(),
                regra.getVigenciaInicio(),
                regra.getVigenciaFim(),
                regra.isAtiva(),
                usos);
    }
}
