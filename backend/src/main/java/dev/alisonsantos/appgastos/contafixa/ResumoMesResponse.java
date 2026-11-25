package dev.alisonsantos.appgastos.contafixa;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * As contas fixas de um mês, com os totais.
 *
 * @param totalPrevisto soma do valor efetivo (pago, ou previsto se pendente)
 * @param totalPago     soma do que já foi pago
 * @param semValor      contas variáveis ainda sem valor nenhum (sem previsão e sem pagamento)
 */
public record ResumoMesResponse(
        YearMonth competencia,
        BigDecimal totalPrevisto,
        BigDecimal totalPago,
        long pagas,
        long pendentes,
        long atrasadas,
        long semValor,
        List<ContaFixaMesResponse> contas
) {
}
