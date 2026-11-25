package dev.alisonsantos.appgastos.contafixa;

import java.time.YearMonth;
import java.util.List;

/**
 * Resultado da busca dos pagamentos de um mês.
 *
 * @param encontrados os pagamentos achados nesta rodada
 * @param mes         o mês completo, já atualizado
 */
public record ResultadoConciliacao(
        YearMonth competencia,
        List<VinculoResponse> encontrados,
        ResumoMesResponse mes
) {
}
