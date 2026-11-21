package dev.alisonsantos.appgastos.transacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Uma transação como a API devolve para o front.
 *
 * A API nunca devolve a entidade diretamente: o DTO controla exatamente
 * o que sai, evita expor detalhes internos e não quebra se a entidade mudar.
 */
public record TransacaoResponse(
        Long id,
        Instant data,
        String descricao,
        String descricaoBanco,
        BigDecimal valor,
        String status,
        String tipoLancamento,
        boolean contaNosGastos,
        String conta,
        String cartao,
        String parcela,
        LocalDate faturaVencimento,
        String categoria,
        String estabelecimento
) {

    public static TransacaoResponse de(Transacao t) {
        String parcela = t.getParcelaTotal() == null ? null
                : t.getParcelaNumero() + "/" + t.getParcelaTotal();

        return new TransacaoResponse(
                t.getId(),
                t.getDataTransacao(),
                t.getDescricaoExibicao(),
                t.getDescricaoBanco(),
                t.getValor(),
                t.getStatus().name(),
                t.getTipoLancamento().name(),
                t.isContaNosGastos(),
                t.getConta().getNome(),
                t.getCartao() == null ? null : t.getCartao().getUltimosDigitos(),
                parcela,
                t.getFatura() == null ? null : t.getFatura().getDataVencimento(),
                t.getCategoriaPluggy(),
                t.getMerchantNome());
    }
}
