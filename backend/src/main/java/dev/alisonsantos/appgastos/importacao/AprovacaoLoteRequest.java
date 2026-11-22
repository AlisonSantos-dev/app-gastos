package dev.alisonsantos.appgastos.importacao;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Várias sugestões aprovadas de uma vez, exatamente como foram sugeridas. */
public record AprovacaoLoteRequest(

        @NotEmpty(message = "informe pelo menos um id")
        @Size(max = 300, message = "no máximo 300 de uma vez")
        List<Long> ids
) {
}
