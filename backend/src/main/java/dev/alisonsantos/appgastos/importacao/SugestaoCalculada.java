package dev.alisonsantos.appgastos.importacao;

import java.time.LocalDate;

/**
 * Uma regra sugerida a partir da planilha, antes de ser gravada.
 *
 * @param valorChave          o trecho da descrição do banco, normalizado
 * @param descricao           o nome que você mais usou para essa descrição
 * @param compras             em quantas compras diferentes ela apareceu
 * @param confiancaDescricao  % das compras em que você usou esse nome
 * @param secao               a seção (de quem) mais frequente
 * @param confiancaSecao      % das compras nessa seção
 * @param transacoesAfetadas  quantas transações do app, hoje sem regra, ela identificaria
 * @param ultimaCompra        a compra mais recente com essa descrição
 */
public record SugestaoCalculada(
        String valorChave,
        String descricao,
        int compras,
        double confiancaDescricao,
        SecaoPlanilha secao,
        double confiancaSecao,
        int transacoesAfetadas,
        LocalDate ultimaCompra
) {
}
