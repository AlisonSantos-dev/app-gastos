package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Resposta paginada por cursor, usada em /v2/transactions.
 *
 * "next" traz os parâmetros para buscar a próxima página.
 * Quando vem null, acabaram as páginas.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyCursorResponse<T>(
        List<T> results,
        String next
) {
}
