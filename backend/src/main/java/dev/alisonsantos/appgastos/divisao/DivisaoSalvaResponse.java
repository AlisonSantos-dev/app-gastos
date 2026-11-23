package dev.alisonsantos.appgastos.divisao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Uma divisão salva como a API devolve. No modo IGUAL, o percentual de
 * cada pessoa é calculado (ex.: 20% para 5 pessoas), só para mostrar.
 */
public record DivisaoSalvaResponse(
        Long id,
        String nome,
        String modo,
        boolean ativa,
        List<Parte> partes
) {

    public record Parte(Long pessoaId, String pessoa, BigDecimal percentual) {
    }

    public static DivisaoSalvaResponse de(DivisaoSalva divisao) {
        int quantidade = divisao.getPartes().size();
        BigDecimal igual = quantidade == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(100).divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);

        return new DivisaoSalvaResponse(
                divisao.getId(),
                divisao.getNome(),
                divisao.getModo().name(),
                divisao.isAtiva(),
                divisao.getPartes().stream()
                        .map(parte -> new Parte(
                                parte.getPessoa().getId(),
                                parte.getPessoa().getNome(),
                                parte.getPercentual() != null ? parte.getPercentual() : igual))
                        .toList());
    }
}
