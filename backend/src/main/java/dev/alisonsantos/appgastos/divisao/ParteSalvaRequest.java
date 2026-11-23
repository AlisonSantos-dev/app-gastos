package dev.alisonsantos.appgastos.divisao;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Uma pessoa da divisão salva (o percentual só no modo PERCENTUAL). */
public record ParteSalvaRequest(

        @NotNull(message = "é obrigatório")
        Long pessoaId,

        BigDecimal percentual
) {
}
