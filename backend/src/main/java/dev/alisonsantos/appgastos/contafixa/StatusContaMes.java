package dev.alisonsantos.appgastos.contafixa;

/**
 * A situação gravada da conta do mês.
 *
 * PENDENTE:   ainda não foi paga.
 * PAGA:       paga (o pagamento foi encontrado no extrato ou informado à mão).
 * DISPENSADA: não vai ter neste mês (ex.: conta isenta, mês sem cobrança).
 *
 * "ATRASADA" não é gravada: é uma PENDENTE cujo vencimento já passou,
 * e por isso é calculada na hora (ver ContaFixaMes.situacao).
 */
public enum StatusContaMes {
    PENDENTE,
    PAGA,
    DISPENSADA
}
