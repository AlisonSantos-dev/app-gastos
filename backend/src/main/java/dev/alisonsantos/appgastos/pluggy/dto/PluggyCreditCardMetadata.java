package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Dados extras das transações de cartão de crédito.
 *
 * cardNumber: final do cartão usado (titular ou adicional).
 * billId: fatura a que a transação pertence (vazio na fatura aberta).
 * billForecastDate: NÃO é usado para definir a fatura, porque não é confiável.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyCreditCardMetadata(
        String cardNumber,
        Integer payeeMCC,
        String billId,
        String paymentType,
        Integer installmentNumber,
        Integer totalInstallments,
        Instant purchaseDate,
        Instant transactionDateTime,
        LocalDate billPostDate,
        String billForecastDate,
        String feeType
) {
}
