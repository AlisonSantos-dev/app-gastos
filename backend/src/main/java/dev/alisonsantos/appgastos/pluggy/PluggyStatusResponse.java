package dev.alisonsantos.appgastos.pluggy;

import java.time.Instant;

/**
 * Resposta do endpoint de status da Pluggy.
 * Mostra se a conexão funciona e até quando a chave vale,
 * mas nunca a chave em si.
 */
public record PluggyStatusResponse(
        boolean conectado,
        Instant apiKeyValidaAte,
        String mensagem
) {
}
