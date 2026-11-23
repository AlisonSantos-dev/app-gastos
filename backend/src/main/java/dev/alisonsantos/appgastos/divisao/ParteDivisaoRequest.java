package dev.alisonsantos.appgastos.divisao;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Uma pessoa na divisão. Conforme o modo:
 *   IGUAL:      só o pessoaId
 *   VALOR:      pessoaId + valor
 *   PERCENTUAL: pessoaId + percentual (de 0 a 100)
 */
public record ParteDivisaoRequest(

        @NotNull(message = "é obrigatório")
        Long pessoaId,

        BigDecimal valor,

        BigDecimal percentual
) {
}
