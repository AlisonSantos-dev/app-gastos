package dev.alisonsantos.appgastos.legenda;

/**
 * Resultado da reaplicação da legenda.
 *
 * @param total               transações analisadas
 * @param alteradas           quantas mudaram (descrição, categoria, dono ou regra)
 * @param gastos              quantas são gastos
 * @param gastosIdentificados gastos reconhecidos por alguma regra
 * @param cobertura           % dos gastos identificados (o indicador do painel de legendas)
 */
public record ResumoLegenda(
        int total,
        int alteradas,
        long gastos,
        long gastosIdentificados,
        double cobertura
) {
}
