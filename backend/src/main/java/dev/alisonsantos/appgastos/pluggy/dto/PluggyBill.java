package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Uma fatura de cartão na Pluggy (GET /bills).
 * Só existem aqui as faturas fechadas. A aberta não aparece.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyBill(
        String id,
        Instant dueDate,
        Instant billClosingDate,
        BigDecimal totalAmount,
        String totalAmountCurrencyCode,
        BigDecimal minimumPaymentAmount,
        List<PluggyFinanceCharge> financeCharges
) {
}
