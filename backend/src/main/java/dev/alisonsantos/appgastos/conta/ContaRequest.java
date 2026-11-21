package dev.alisonsantos.appgastos.conta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O que a API recebe para editar uma conta.
 * (Contas não são criadas pela API: elas vêm da sincronização com a Pluggy.)
 *
 * @param banco        ex.: "Nubank"
 * @param nome         ex.: "Cartão Nubank"
 * @param donoPadraoId quem recebe os lançamentos sem cartão; vazio = ninguém
 * @param ativa        opcional; se não vier, não muda
 */
public record ContaRequest(

        @NotBlank(message = "é obrigatório")
        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        String banco,

        @NotBlank(message = "é obrigatório")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String nome,

        Long donoPadraoId,

        Boolean ativa
) {
}
