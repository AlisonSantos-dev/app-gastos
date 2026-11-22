package dev.alisonsantos.appgastos.caixa;

import jakarta.validation.constraints.NotNull;

/** O dono escolhido na caixa de entrada (o clique no botão da pessoa). */
public record DonoRequest(

        @NotNull(message = "é obrigatório")
        Long pessoaId
) {
}
