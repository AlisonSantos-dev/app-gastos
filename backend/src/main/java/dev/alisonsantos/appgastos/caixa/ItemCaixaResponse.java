package dev.alisonsantos.appgastos.caixa;

import dev.alisonsantos.appgastos.cartao.Cartao;
import dev.alisonsantos.appgastos.cartao.ModoDono;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.transacao.Transacao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Um card da caixa de entrada.
 *
 * Traz tudo o que a tela precisa para você resolver a compra sem abrir
 * outra tela: os motivos da pendência, o dono atual e, nos cartões
 * compartilhados, as pessoas candidatas (os botões rápidos) e a sugestão.
 */
public record ItemCaixaResponse(
        Long id,
        Instant data,
        String descricao,
        String descricaoBanco,
        BigDecimal valor,
        String conta,
        String cartao,
        String parcela,
        String categoria,
        List<MotivoPendencia> motivos,
        PessoaResumo pessoa,
        String origemPessoa,
        List<PessoaResumo> candidatos,
        PessoaResumo sugestao
) {

    public record PessoaResumo(Long id, String nome, String cor) {

        static PessoaResumo de(Pessoa pessoa) {
            return pessoa == null ? null : new PessoaResumo(pessoa.getId(), pessoa.getNome(), pessoa.getCor());
        }
    }

    public static ItemCaixaResponse de(Transacao t) {
        Cartao cartao = t.getCartao();
        boolean compartilhado = cartao != null && cartao.getModoDono() == ModoDono.PERGUNTAR;

        List<PessoaResumo> candidatos = compartilhado
                ? cartao.getCandidatos().stream()
                        .sorted(Comparator.comparing(Pessoa::getNome))
                        .map(PessoaResumo::de)
                        .toList()
                : List.of();

        return new ItemCaixaResponse(
                t.getId(),
                t.getDataTransacao(),
                t.getDescricaoExibicao(),
                t.getDescricaoBanco(),
                t.getValor(),
                t.getConta().getNome(),
                cartao == null ? null : cartao.getUltimosDigitos(),
                t.getParcelaTotal() == null ? null : t.getParcelaNumero() + "/" + t.getParcelaTotal(),
                t.getCategoria(),
                MotivoPendencia.de(t),
                PessoaResumo.de(t.getPessoa()),
                t.getOrigemPessoa() == null ? null : t.getOrigemPessoa().name(),
                candidatos,
                compartilhado ? PessoaResumo.de(cartao.getDonoPadrao()) : null);
    }
}
