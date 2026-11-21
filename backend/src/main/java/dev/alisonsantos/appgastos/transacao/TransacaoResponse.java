package dev.alisonsantos.appgastos.transacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Uma transação como a API devolve para o front.
 *
 * A API nunca devolve a entidade diretamente: o DTO controla exatamente
 * o que sai, evita expor detalhes internos e não quebra se a entidade mudar.
 *
 * Os campos "origem..." dizem de onde veio cada decisão (BANCO, REGRA,
 * CARTAO, MANUAL...): é o que permite à tela mostrar, por exemplo,
 * "via regra" ou "via cartão" ao lado do dono.
 */
public record TransacaoResponse(
        Long id,
        Instant data,
        String descricao,
        String descricaoBanco,
        String origemDescricao,
        BigDecimal valor,
        String status,
        String tipoLancamento,
        boolean contaNosGastos,
        String pessoa,
        String origemPessoa,
        String categoria,
        Long regraId,
        String conta,
        String cartao,
        String parcela,
        LocalDate faturaVencimento,
        String categoriaPluggy,
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
                t.getOrigemDescricao().name(),
                t.getValor(),
                t.getStatus().name(),
                t.getTipoLancamento().name(),
                t.isContaNosGastos(),
                t.getPessoa() == null ? null : t.getPessoa().getNome(),
                t.getOrigemPessoa() == null ? null : t.getOrigemPessoa().name(),
                t.getCategoria(),
                t.getRegra() == null ? null : t.getRegra().getId(),
                t.getConta().getNome(),
                t.getCartao() == null ? null : t.getCartao().getUltimosDigitos(),
                parcela,
                t.getFatura() == null ? null : t.getFatura().getDataVencimento(),
                t.getCategoriaPluggy(),
                t.getMerchantNome());
    }
}
