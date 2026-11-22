package dev.alisonsantos.appgastos.caixa;

/**
 * Os números da caixa de entrada (para o "badge" na tela inicial).
 *
 * @param total      compras pendentes
 * @param semDono    pendentes sem dono
 * @param semLegenda pendentes sem legenda
 */
public record ResumoCaixaResponse(long total, long semDono, long semLegenda) {
}
