package dev.alisonsantos.appgastos.contafixa;

import jakarta.validation.constraints.NotNull;

/** O pagamento escolhido à mão para a conta do mês. */
public record VincularRequest(

        @NotNull(message = "é obrigatório")
        Long transacaoId
) {
}
