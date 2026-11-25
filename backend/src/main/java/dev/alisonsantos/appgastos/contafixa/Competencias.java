package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.comum.RegraNegocioException;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Lê a competência que vem na URL, no formato "2026-11".
 */
final class Competencias {

    private Competencias() {
    }

    static YearMonth ler(String texto) {
        try {
            return YearMonth.parse(texto);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Competência inválida: \"" + texto + "\". Use o formato AAAA-MM (ex.: 2026-11).");
        }
    }
}
