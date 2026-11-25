package dev.alisonsantos.appgastos.comum;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Lê a competência que vem na URL, no formato "2026-11".
 */
public final class Competencia {

    private Competencia() {
    }

    public static YearMonth ler(String texto) {
        try {
            return YearMonth.parse(texto);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Competência inválida: \"" + texto + "\". Use o formato AAAA-MM (ex.: 2026-11).");
        }
    }
}
