package dev.alisonsantos.appgastos.pessoa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * O que a API recebe para criar ou editar uma pessoa.
 *
 * As anotações de validação são conferidas antes de chegar no service
 * (por causa do @Valid no controller). Se algo falhar, a resposta é um 400
 * com a mensagem de cada campo, montada pelo ApiExceptionHandler.
 *
 * @param nome          obrigatório, até 100 caracteres
 * @param tipo          PESSOA ou GRUPO
 * @param cor           opcional, em hexadecimal (ex.: #60A5FA)
 * @param nomeNoExtrato opcional: como o nome aparece nos Pix recebidos
 * @param ativo         opcional: na criação, se não vier, a pessoa nasce ativa
 */
public record PessoaRequest(

        @NotBlank(message = "é obrigatório")
        @Size(max = 100, message = "deve ter no máximo 100 caracteres")
        String nome,

        @NotNull(message = "é obrigatório (PESSOA ou GRUPO)")
        TipoPessoa tipo,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "deve estar no formato #RRGGBB (ex.: #60A5FA)")
        String cor,

        @Size(max = 150, message = "deve ter no máximo 150 caracteres")
        String nomeNoExtrato,

        Boolean ativo
) {
}
