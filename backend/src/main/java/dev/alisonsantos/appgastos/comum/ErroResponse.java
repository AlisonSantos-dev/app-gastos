package dev.alisonsantos.appgastos.comum;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * O formato padrão de todos os erros da API.
 *
 * Qualquer erro (validação, não encontrado, regra de negócio) volta neste
 * mesmo formato, o que facilita muito para o front: ele sempre sabe onde
 * achar a mensagem.
 *
 * @param status   o código HTTP (400, 404, 422...)
 * @param erro     o tipo do erro, curto
 * @param mensagem a explicação em português
 * @param campos   os erros por campo, nas validações (ex.: "nome" -> "é obrigatório")
 * @param instante quando aconteceu
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        Map<String, String> campos,
        Instant instante
) {

    public static ErroResponse de(int status, String erro, String mensagem) {
        return new ErroResponse(status, erro, mensagem, null, Instant.now());
    }

    public static ErroResponse deCampos(String mensagem, Map<String, String> campos) {
        return new ErroResponse(400, "Dados inválidos", mensagem, campos, Instant.now());
    }
}
