package dev.alisonsantos.appgastos.caixa;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Limpeza do histórico: confirma todos os gastos ATÉ esta data, inclusive.
 * Ex.: tudo o que você já conferiu na planilha.
 */
public record ConfirmarAteRequest(

        @NotNull(message = "é obrigatória (ex.: 2026-10-30)")
        LocalDate data
) {
}
