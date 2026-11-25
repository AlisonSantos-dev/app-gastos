package dev.alisonsantos.appgastos.contafixa;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Criar ou editar uma conta fixa.
 *
 * Exemplos:
 *   Financiamento (valor fixo, dividido entre os pais):
 *   { "nome": "Financiamento Caixa", "grupo": "Casa dos pais", "contaId": 1,
 *     "diaVencimento": 17, "valorEsperado": 558.30, "divisaoSalvaId": 2,
 *     "chavePagamento": "HABITACAO", "categoria": "Moradia", "inicio": "2026-01" }
 *
 *   Sanepar (valor variável, de uma pessoa):
 *   { "nome": "Sanepar", "grupo": "Casa dos pais", "contaId": 1, "diaVencimento": 17,
 *     "pessoaId": 3, "chavePagamento": "SANEPAR", "categoria": "Água", "inicio": "2026-01" }
 *
 * @param valorEsperado vazio = conta variável (o previsto vira o último valor pago)
 * @param inicio        o primeiro mês, no formato "AAAA-MM"
 * @param fim           o último mês, se a conta tiver fim (ex.: financiamento quitado)
 */
public record ContaFixaRequest(

        @NotBlank(message = "é obrigatório")
        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        String nome,

        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        String grupo,

        Long contaId,

        @NotNull(message = "é obrigatório")
        @Min(value = 1, message = "deve ser entre 1 e 31")
        @Max(value = 31, message = "deve ser entre 1 e 31")
        Integer diaVencimento,

        @DecimalMin(value = "0.01", message = "deve ser maior que zero")
        BigDecimal valorEsperado,

        Long pessoaId,

        Long divisaoSalvaId,

        @Size(max = 255, message = "deve ter no máximo 255 caracteres")
        String chavePagamento,

        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        String categoria,

        @NotNull(message = "é obrigatório (formato AAAA-MM, ex.: 2026-11)")
        YearMonth inicio,

        YearMonth fim
) {
}
