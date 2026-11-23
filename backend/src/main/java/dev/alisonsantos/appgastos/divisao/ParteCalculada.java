package dev.alisonsantos.appgastos.divisao;

import dev.alisonsantos.appgastos.pessoa.Pessoa;

import java.math.BigDecimal;

/**
 * Uma parte da divisão, já calculada: quem, quanto e em que proporção.
 *
 * @param fracao de 0 a 1 (ex.: 0.2 para 20%)
 */
public record ParteCalculada(Pessoa pessoa, BigDecimal valor, BigDecimal fracao) {
}
