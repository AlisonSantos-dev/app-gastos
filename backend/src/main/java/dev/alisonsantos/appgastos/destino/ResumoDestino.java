package dev.alisonsantos.appgastos.destino;

import java.util.Map;

/**
 * Resultado da redistribuição dos destinos.
 *
 * @param total         transações analisadas
 * @param alteradas     quantas mudaram de dono
 * @param porPessoa     quantos gastos ficaram com cada pessoa
 * @param semDono       gastos ainda sem dono (vão para a caixa de entrada)
 */
public record ResumoDestino(
        int total,
        int alteradas,
        Map<String, Long> porPessoa,
        long semDono
) {
}
