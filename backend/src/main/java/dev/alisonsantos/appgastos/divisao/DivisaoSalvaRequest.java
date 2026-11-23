package dev.alisonsantos.appgastos.divisao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Criar ou editar uma divisão salva.
 *
 * Exemplo:
 *   { "nome": "Spotify Família", "modo": "IGUAL",
 *     "partes": [{"pessoaId": 1}, {"pessoaId": 2}, {"pessoaId": 5}, {"pessoaId": 6}, {"pessoaId": 7}] }
 */
public record DivisaoSalvaRequest(

        @NotBlank(message = "é obrigatório")
        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        String nome,

        @NotNull(message = "é obrigatório (IGUAL ou PERCENTUAL)")
        ModoDivisao modo,

        @NotNull(message = "é obrigatório")
        @Size(min = 2, max = 20, message = "informe de 2 a 20 pessoas")
        List<@Valid ParteSalvaRequest> partes
) {
}
