package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Uma transação na Pluggy (GET /v2/transactions).
 *
 * Só os campos que o app usa são mapeados. O resto é ignorado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyTransaction(
        String id,
        String accountId,
        String description,
        String descriptionRaw,
        String currencyCode,
        BigDecimal amount,
        Instant date,
        String category,
        String categoryId,
        String status,
        String type,
        String operationType,
        PluggyCreditCardMetadata creditCardMetadata,
        PluggyMerchant merchant,
        PluggyPaymentData paymentData,
        Instant createdAt,
        Instant updatedAt
) {
}
