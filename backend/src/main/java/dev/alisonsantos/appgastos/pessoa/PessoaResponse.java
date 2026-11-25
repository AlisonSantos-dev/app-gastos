package dev.alisonsantos.appgastos.pessoa;

/**
 * Uma pessoa como a API devolve.
 */
public record PessoaResponse(
        Long id,
        String nome,
        String tipo,
        String cor,
        String nomeNoExtrato,
        boolean ativo,
        boolean titular
) {

    public static PessoaResponse de(Pessoa pessoa) {
        return new PessoaResponse(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getTipo().name(),
                pessoa.getCor(),
                pessoa.getNomeNoExtrato(),
                pessoa.isAtivo(),
                pessoa.isTitular());
    }
}
