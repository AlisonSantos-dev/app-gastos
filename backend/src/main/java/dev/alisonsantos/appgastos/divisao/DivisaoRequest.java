package dev.alisonsantos.appgastos.divisao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * O pedido de divisão de uma compra.
 *
 * Exemplos:
 *   { "modo": "IGUAL", "partes": [{"pessoaId": 1}, {"pessoaId": 2}] }
 *
 *   { "modo": "PERCENTUAL", "partes": [
 *       {"pessoaId": 3, "percentual": 50}, {"pessoaId": 4, "percentual": 50}] }
 *
 *   { "modo": "VALOR", "partes": [
 *       {"pessoaId": 1, "valor": 60.00}, {"pessoaId": 2, "valor": 40.00}] }
 *
 * @param propagarParcelas se a divisão vale para todas as parcelas da compra (padrão: sim)
 */
public record DivisaoRequest(

        @NotNull(message = "é obrigatório (IGUAL, VALOR ou PERCENTUAL)")
        ModoDivisao modo,

        @NotNull(message = "é obrigatório")
        @Size(min = 2, max = 20, message = "informe de 2 a 20 pessoas")
        List<@Valid ParteDivisaoRequest> partes,

        Boolean propagarParcelas
) {

    boolean devePropagar() {
        return !Boolean.FALSE.equals(propagarParcelas);
    }
}
