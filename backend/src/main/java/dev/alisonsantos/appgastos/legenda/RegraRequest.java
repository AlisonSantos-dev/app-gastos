package dev.alisonsantos.appgastos.legenda;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * O que a API recebe para criar ou editar uma regra.
 *
 * Exemplos:
 *   { "tipoChave": "DESCRICAO_CONTEM", "valorChave": "wellhub",
 *     "descricao": "Wellhub", "categoria": "Academia" }
 *
 *   { "tipoChave": "DESCRICAO_CONTEM", "valorChave": "wellhub gabriella", "pessoaId": 2 }
 *
 *   { "tipoChave": "DESCRICAO_CONTEM", "valorChave": "uber", "cartaoFinal": "1870",
 *     "pessoaId": 4, "vigenciaInicio": "2026-12-01", "vigenciaFim": "2026-12-31" }
 *
 *   { "tipoChave": "CNPJ", "valorChave": "17687734000193", "descricao": "Spotify Família",
 *     "divisaoSalvaId": 1 }       (divide a compra, em vez de dar um dono)
 */
public record RegraRequest(

        @NotNull(message = "é obrigatório (CNPJ, MERCHANT, DESCRICAO_IGUAL, DESCRICAO_CONTEM ou MCC)")
        TipoChave tipoChave,

        @NotBlank(message = "é obrigatório")
        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String valorChave,

        @Pattern(regexp = "^\\d{4}$", message = "deve ter exatamente 4 dígitos")
        String cartaoFinal,

        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String descricao,

        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        String categoria,

        Long pessoaId,

        Integer prioridade,

        LocalDate vigenciaInicio,

        LocalDate vigenciaFim,

        Long divisaoSalvaId
) {
}
