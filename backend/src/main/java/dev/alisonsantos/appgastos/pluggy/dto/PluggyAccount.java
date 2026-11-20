package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Uma conta na Pluggy (GET /accounts).
 *
 * type: BANK (conta corrente) ou CREDIT (cartão de crédito).
 * number: no cartão, o final do titular; na conta corrente, o número
 * da conta. Por isso nunca é exibido inteiro.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyAccount(
        String id,
        String itemId,
        String type,
        String subtype,
        String name,
        String marketingName,
        String number,
        BigDecimal balance,
        String currencyCode
) {
}
