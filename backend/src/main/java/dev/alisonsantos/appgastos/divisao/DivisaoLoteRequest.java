package dev.alisonsantos.appgastos.divisao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Várias compras divididas de uma vez (um "evento").
 *
 * Com uma divisão salva:
 *   { "transacaoIds": [101, 102, 103], "divisaoSalvaId": 1 }
 *
 * Ou informando a divisão (só IGUAL ou PERCENTUAL):
 *   { "transacaoIds": [101, 102, 103], "evento": "Churrasco 15/11",
 *     "modo": "IGUAL", "partes": [{"pessoaId": 1}, {"pessoaId": 2}, {"pessoaId": 3}] }
 *
 * @param evento opcional: vira a observação de todas as compras do evento
 */
public record DivisaoLoteRequest(

        @NotEmpty(message = "informe pelo menos uma transação")
        @Size(max = 200, message = "no máximo 200 de uma vez")
        List<Long> transacaoIds,

        Long divisaoSalvaId,

        ModoDivisao modo,

        @Size(min = 2, max = 20, message = "informe de 2 a 20 pessoas")
        List<@Valid ParteDivisaoRequest> partes,

        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String evento
) {
}
