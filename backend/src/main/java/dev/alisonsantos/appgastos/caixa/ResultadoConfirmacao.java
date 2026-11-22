package dev.alisonsantos.appgastos.caixa;

import java.util.List;

/**
 * Resultado de uma confirmação.
 *
 * @param confirmadas quantas compras saíram da caixa (inclui as outras parcelas)
 * @param avisos      as que não puderam ser confirmadas, e o motivo
 */
public record ResultadoConfirmacao(int confirmadas, List<String> avisos) {
}
