package dev.alisonsantos.appgastos.importacao;

/**
 * Resultado da leitura da planilha.
 *
 * @param comprasLidas          linhas de compra encontradas nas abas
 * @param sugestoesCriadas      sugestões novas, esperando aprovação
 * @param jaCobertasPorRegra    sugestões ignoradas porque já existe uma regra igual
 * @param comPessoa             quantas sugestões também indicam o dono
 */
public record ResumoImportacao(
        int comprasLidas,
        int sugestoesCriadas,
        int jaCobertasPorRegra,
        long comPessoa
) {
}
