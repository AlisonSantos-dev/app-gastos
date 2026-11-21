package dev.alisonsantos.appgastos.conta;

import java.util.List;

/**
 * Uma conta com os seus cartões, como a API devolve para o front.
 */
public record ContaResponse(
        Long id,
        String banco,
        String nome,
        String tipo,
        boolean ativa,
        List<CartaoResumo> cartoes
) {

    /** Um cartão da conta. Só os 4 últimos dígitos, como sempre. */
    public record CartaoResumo(
            Long id,
            String ultimosDigitos,
            String apelido,
            String tipo,
            String modoDono,
            String status
    ) {
    }
}
