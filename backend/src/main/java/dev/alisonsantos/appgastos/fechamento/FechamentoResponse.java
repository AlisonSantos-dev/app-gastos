package dev.alisonsantos.appgastos.fechamento;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

/**
 * O fechamento de um mês.
 *
 * @param status      PREVIA (calculado agora, pode mudar) ou PUBLICADO (a foto congelada)
 * @param semDono     o total das linhas sem dono (null se não houver)
 * @param conferencia as faturas do mês contra o total do banco (só na prévia)
 * @param alertas     o que conferir antes de publicar (só na prévia)
 */
public record FechamentoResponse(
        YearMonth competencia,
        String status,
        Instant publicadoEm,
        BigDecimal totalGeral,
        List<TotalPessoa> pessoas,
        TotalPessoa semDono,
        List<ConferenciaFatura> conferencia,
        List<Alerta> alertas
) {
}
