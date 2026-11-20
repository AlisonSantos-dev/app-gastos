package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Resposta paginada por número de página, usada em /accounts e /bills.
 *
 * Genérico (<T>): o mesmo record serve para uma lista de contas ou
 * de faturas. O tipo é definido na hora da chamada.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyPaginaResponse<T>(
        Integer total,
        Integer totalPages,
        Integer page,
        List<T> results
) {
}
