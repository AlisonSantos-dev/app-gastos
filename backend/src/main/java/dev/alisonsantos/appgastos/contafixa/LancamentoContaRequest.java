package dev.alisonsantos.appgastos.contafixa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ajustes manuais na conta de um mês. Todos opcionais (PATCH).
 *
 * Exemplos:
 *   Paga fora do extrato (ex.: na lotérica):  { "valorPago": 89.43, "dataPagamento": "2026-11-17" }
 *   Ajustar a previsão:                       { "valorPrevisto": 95.00 }
 *   Não tem neste mês:                        { "dispensar": true }
 *   Desfazer pagamento ou dispensa:           { "reabrir": true }
 */
public record LancamentoContaRequest(

        @DecimalMin(value = "0.01", message = "deve ser maior que zero")
        BigDecimal valorPrevisto,

        @DecimalMin(value = "0.01", message = "deve ser maior que zero")
        BigDecimal valorPago,

        LocalDate dataPagamento,

        Boolean dispensar,

        Boolean reabrir,

        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String observacao
) {
}
