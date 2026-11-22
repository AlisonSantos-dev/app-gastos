package dev.alisonsantos.appgastos.caixa;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Várias compras confirmadas de uma vez (seleção múltipla na tela). */
public record ConfirmarRequest(

        @NotEmpty(message = "informe pelo menos um id")
        @Size(max = 500, message = "no máximo 500 de uma vez")
        List<Long> ids
) {
}
