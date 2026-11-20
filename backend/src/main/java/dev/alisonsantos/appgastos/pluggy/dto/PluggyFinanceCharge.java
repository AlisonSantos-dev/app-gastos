package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Um encargo da fatura (juros, IOF, multa...). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyFinanceCharge(
        String type,
        BigDecimal amount,
        String additionalInfo
) {
}
