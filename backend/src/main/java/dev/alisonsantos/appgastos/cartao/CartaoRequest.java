package dev.alisonsantos.appgastos.cartao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * O que a API recebe para configurar um cartão.
 * (Cartões não são criados pela API: eles são descobertos pela sincronização.)
 *
 * Dois jeitos de configurar:
 *
 * FIXO: toda compra vai para uma pessoa.
 *   { "modoDono": "FIXO", "donoPadraoId": 2 }
 *
 * PERGUNTAR: cartão compartilhado; cada compra vai para a caixa de entrada,
 * com as candidatas como botões. O donoPadraoId é opcional e vira a sugestão.
 *   { "modoDono": "PERGUNTAR", "candidatosIds": [3, 4], "donoPadraoId": 3 }
 */
public record CartaoRequest(

        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        String apelido,

        TipoCartao tipo,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "deve estar no formato #RRGGBB (ex.: #60A5FA)")
        String cor,

        @NotNull(message = "é obrigatório (FIXO ou PERGUNTAR)")
        ModoDono modoDono,

        Long donoPadraoId,

        List<Long> candidatosIds
) {
}
