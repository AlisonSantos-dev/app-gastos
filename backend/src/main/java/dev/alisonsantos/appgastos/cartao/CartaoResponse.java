package dev.alisonsantos.appgastos.cartao;

import dev.alisonsantos.appgastos.pessoa.Pessoa;

import java.util.Comparator;
import java.util.List;

/**
 * Um cartão com a sua configuração completa.
 */
public record CartaoResponse(
        Long id,
        Long contaId,
        String conta,
        String ultimosDigitos,
        String apelido,
        String tipo,
        String cor,
        String modoDono,
        String status,
        PessoaResumo donoPadrao,
        List<PessoaResumo> candidatos
) {

    /** Só o essencial de uma pessoa, para mostrar no cartão. */
    public record PessoaResumo(Long id, String nome, String cor) {

        static PessoaResumo de(Pessoa pessoa) {
            return pessoa == null ? null : new PessoaResumo(pessoa.getId(), pessoa.getNome(), pessoa.getCor());
        }
    }

    public static CartaoResponse de(Cartao cartao) {
        return new CartaoResponse(
                cartao.getId(),
                cartao.getConta().getId(),
                cartao.getConta().getNome(),
                cartao.getUltimosDigitos(),
                cartao.getApelido(),
                cartao.getTipo() == null ? null : cartao.getTipo().name(),
                cartao.getCor(),
                cartao.getModoDono().name(),
                cartao.getStatus().name(),
                PessoaResumo.de(cartao.getDonoPadrao()),
                cartao.getCandidatos().stream()
                        .sorted(Comparator.comparing(Pessoa::getNome))
                        .map(PessoaResumo::de)
                        .toList());
    }
}
