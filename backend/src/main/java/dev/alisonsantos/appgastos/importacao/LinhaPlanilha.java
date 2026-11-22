package dev.alisonsantos.appgastos.importacao;

import java.time.LocalDate;

/**
 * Uma compra lida da planilha.
 *
 * @param aba                 o nome da aba (ex.: "NOVEMBRO-2026")
 * @param secao               a seção em que ela estava (de quem era)
 * @param dataCompra          a data da compra (pode faltar nas recorrentes)
 * @param descricaoAmigavel   o que você escreveu (coluna "Categoria")
 * @param descricaoFornecedor como estava no banco (coluna "Descrição Fornecedor")
 */
public record LinhaPlanilha(
        String aba,
        SecaoPlanilha secao,
        LocalDate dataCompra,
        String descricaoAmigavel,
        String descricaoFornecedor
) {
}
