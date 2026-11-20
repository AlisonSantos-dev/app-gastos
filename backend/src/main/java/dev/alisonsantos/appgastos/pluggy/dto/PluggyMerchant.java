package dev.alisonsantos.appgastos.pluggy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Dados do estabelecimento, quando a Pluggy consegue identificar.
 * Em muitas compras vem null, ou com o CNPJ vazio.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluggyMerchant(
        String name,
        String businessName,
        String cnpj,
        String cnae,
        String category
) {
}
