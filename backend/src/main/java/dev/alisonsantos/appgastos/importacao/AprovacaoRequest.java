package dev.alisonsantos.appgastos.importacao;

import jakarta.validation.constraints.Size;

/**
 * Ajustes opcionais ao aprovar uma sugestão.
 * Se não vier nada, a regra é criada exatamente como foi sugerida.
 *
 * @param descricao    outro nome, se quiser corrigir o sugerido
 * @param categoria    uma categoria para a regra (a planilha não tinha)
 * @param pessoaId     outro dono (ou um dono, se a sugestão não tinha)
 * @param semPessoa    true para criar a regra SEM dono, mesmo que a sugestão tenha
 */
public record AprovacaoRequest(

        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String descricao,

        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        String categoria,

        Long pessoaId,

        Boolean semPessoa
) {
}
