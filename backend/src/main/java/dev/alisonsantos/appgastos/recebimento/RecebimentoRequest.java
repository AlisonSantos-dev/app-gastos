package dev.alisonsantos.appgastos.recebimento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Registrar um recebimento.
 *
 * A partir de um Pix do extrato (o "tique" da sugestão): o valor e a data vêm do Pix.
 *   { "pessoaId": 3, "competencia": "2026-11", "transacaoId": 5021 }
 *
 * Em dinheiro (ou outro meio, sem extrato):
 *   { "pessoaId": 4, "competencia": "2026-11", "valor": 300.00, "data": "2026-11-20",
 *     "observacao": "Em dinheiro" }
 */
public record RecebimentoRequest(

        @NotNull(message = "é obrigatório")
        Long pessoaId,

        @NotNull(message = "é obrigatório (formato AAAA-MM, ex.: 2026-11)")
        YearMonth competencia,

        @DecimalMin(value = "0.01", message = "deve ser maior que zero")
        BigDecimal valor,

        LocalDate data,

        Long transacaoId,

        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String observacao
) {
}
