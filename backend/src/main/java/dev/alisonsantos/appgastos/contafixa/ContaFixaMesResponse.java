package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.divisao.CalculadoraDivisao;
import dev.alisonsantos.appgastos.divisao.DivisaoSalva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * A conta fixa de um mês, como a API devolve.
 *
 * @param valorEfetivo o pago, ou o previsto se ainda não pagou (é o que entra no fechamento)
 * @param partes       quanto fica para cada pessoa, sobre o valor efetivo
 */
public record ContaFixaMesResponse(
        Long id,
        Long contaFixaId,
        String nome,
        String grupo,
        YearMonth competencia,
        LocalDate vencimento,
        BigDecimal valorPrevisto,
        BigDecimal valorPago,
        BigDecimal valorEfetivo,
        LocalDate dataPagamento,
        String situacao,
        Long transacaoId,
        String observacao,
        List<Parte> partes
) {

    public record Parte(Long pessoaId, String pessoa, BigDecimal valor) {
    }

    public static ContaFixaMesResponse de(ContaFixaMes mes, LocalDate hoje) {
        ContaFixa conta = mes.getContaFixa();
        return new ContaFixaMesResponse(
                mes.getId(),
                conta.getId(),
                conta.getNome(),
                conta.getGrupo(),
                mes.getCompetencia(),
                mes.getVencimento(),
                mes.getValorPrevisto(),
                mes.getValorPago(),
                mes.valorEfetivo(),
                mes.getDataPagamento(),
                mes.situacao(hoje).name(),
                mes.getTransacao() == null ? null : mes.getTransacao().getId(),
                mes.getObservacao(),
                partes(conta, mes.valorEfetivo()));
    }

    /** A parte de cada pessoa: tudo para a pessoa da conta, ou a divisão salva aplicada ao valor. */
    static List<Parte> partes(ContaFixa conta, BigDecimal valor) {
        if (valor == null || valor.signum() == 0) {
            return List.of();
        }
        if (conta.getPessoa() != null) {
            return List.of(new Parte(conta.getPessoa().getId(), conta.getPessoa().getNome(), valor));
        }
        DivisaoSalva divisao = conta.getDivisaoSalva();
        try {
            return CalculadoraDivisao.calcular(divisao.getModo(), divisao.paraEntradas(), valor).stream()
                    .map(p -> new Parte(p.pessoa().getId(), p.pessoa().getNome(), p.valor()))
                    .toList();
        } catch (RegraNegocioException e) {
            return List.of();
        }
    }
}
