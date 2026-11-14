package dev.alisonsantos.appgastos.pluggy.dto;

/**
 * Corpo do POST /auth da Pluggy.
 *
 * Record: uma classe só de dados, imutável, com construtor, getters,
 * equals e toString gerados pelo Java. Perfeito para DTOs.
 */
public record AuthRequest(String clientId, String clientSecret) {

    /** Nunca mostrar o secret em logs, nem por acidente. */
    @Override
    public String toString() {
        return "AuthRequest[clientId=" + clientId + ", clientSecret=***]";
    }
}
