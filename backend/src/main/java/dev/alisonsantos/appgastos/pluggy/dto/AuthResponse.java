package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resposta do POST /auth da Pluggy.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true): se a Pluggy passar a mandar
 * campos novos, o app não quebra. Só lê o que precisa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthResponse(String apiKey) {

    /** Nunca mostrar a API Key em logs. */
    @Override
    public String toString() {
        return "AuthResponse[apiKey=***]";
    }
}
