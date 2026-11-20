package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Dados de pagamento de Pix, TED e boletos (quem pagou e quem recebeu).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyPaymentData(
        PluggyParticipante payer,
        PluggyParticipante receiver,
        String paymentMethod
) {
}
