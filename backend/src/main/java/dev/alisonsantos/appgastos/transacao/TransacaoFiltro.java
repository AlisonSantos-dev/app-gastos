package dev.alisonsantos.appgastos.transacao;

import java.time.LocalDate;

/**
 * Os filtros da consulta de transações. Todos são opcionais:
 * só os preenchidos entram na consulta.
 *
 * @param contaId       só de uma conta
 * @param cartao        só de um cartão (os 4 últimos dígitos)
 * @param faturaId      só de uma fatura
 * @param tipo          só de um tipo (COMPRA, ESTORNO...)
 * @param somenteGastos só o que entra nos gastos
 * @param de            a partir desta data (horário de Brasília)
 * @param ate           até esta data, inclusive
 * @param texto         trecho da descrição do banco
 */
public record TransacaoFiltro(
        Long contaId,
        String cartao,
        Long faturaId,
        TipoLancamento tipo,
        boolean somenteGastos,
        LocalDate de,
        LocalDate ate,
        String texto
) {
}
