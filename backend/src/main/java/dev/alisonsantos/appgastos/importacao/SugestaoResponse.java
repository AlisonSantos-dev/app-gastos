package dev.alisonsantos.appgastos.importacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma sugestão como a API devolve.
 *
 * Exemplo: "quando a descrição contiver ATACADISTA BEM BOM, a compra é
 * 'Compras Mercado Mãe', da Marlene (83 compras; nome igual em 86%;
 * identificaria 12 transações do app)".
 */
public record SugestaoResponse(
        Long id,
        String tipoChave,
        String valorChave,
        String descricao,
        Long pessoaId,
        String pessoa,
        String secaoPlanilha,
        int compras,
        BigDecimal confiancaDescricao,
        BigDecimal confiancaPessoa,
        int transacoesAfetadas,
        LocalDate ultimaCompra,
        String status,
        Long regraId
) {

    public static SugestaoResponse de(SugestaoRegra s) {
        return new SugestaoResponse(
                s.getId(),
                s.getTipoChave().name(),
                s.getValorChave(),
                s.getDescricao(),
                s.getPessoa() == null ? null : s.getPessoa().getId(),
                s.getPessoa() == null ? null : s.getPessoa().getNome(),
                s.getSecaoPlanilha() == null ? null : s.getSecaoPlanilha().name(),
                s.getCompras(),
                s.getConfiancaDescricao(),
                s.getConfiancaPessoa(),
                s.getTransacoesAfetadas(),
                s.getUltimaCompra(),
                s.getStatus().name(),
                s.getRegra() == null ? null : s.getRegra().getId());
    }
}
