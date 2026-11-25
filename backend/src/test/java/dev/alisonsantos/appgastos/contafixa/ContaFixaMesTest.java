package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.divisao.DivisaoSalva;
import dev.alisonsantos.appgastos.divisao.ModoDivisao;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.TipoPessoa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes das regras das contas fixas, com as contas reais da família.
 */
@DisplayName("Contas fixas")
class ContaFixaMesTest {

    private final Pessoa marlene = new Pessoa("Marlene", TipoPessoa.PESSOA);
    private final Pessoa nelson = new Pessoa("Nelson", TipoPessoa.PESSOA);

    @Test
    @DisplayName("Vencimento no dia 31 cai no último dia dos meses mais curtos")
    void vencimentoNoFimDoMes() {
        ContaFixa conta = new ContaFixa("Aluguel", 31, YearMonth.of(2026, 1));

        assertThat(conta.vencimentoEm(YearMonth.of(2027, 2))).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(conta.vencimentoEm(YearMonth.of(2026, 11))).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(conta.vencimentoEm(YearMonth.of(2026, 12))).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("A conta só existe entre o mês de início e o mês de fim")
    void vigencia() {
        ContaFixa financiamento = new ContaFixa("Financiamento Caixa", 17, YearMonth.of(2026, 1));
        financiamento.definirVigencia(YearMonth.of(2026, 1), YearMonth.of(2026, 12));

        assertThat(financiamento.isVigenteEm(YearMonth.of(2025, 12))).isFalse();
        assertThat(financiamento.isVigenteEm(YearMonth.of(2026, 11))).isTrue();
        assertThat(financiamento.isVigenteEm(YearMonth.of(2027, 1))).isFalse();
    }

    @Test
    @DisplayName("Pendente com o vencimento passado aparece como ATRASADA")
    void atrasada() {
        ContaFixaMes sanepar = new ContaFixaMes(sanepar(), YearMonth.of(2026, 11), new BigDecimal("89.43"));

        assertThat(sanepar.situacao(LocalDate.of(2026, 11, 10))).isEqualTo(ContaFixaMes.Situacao.PENDENTE);
        assertThat(sanepar.situacao(LocalDate.of(2026, 11, 20))).isEqualTo(ContaFixaMes.Situacao.ATRASADA);
    }

    @Test
    @DisplayName("Depois de paga, vale o valor pago (e não mais o previsto)")
    void valorEfetivo() {
        ContaFixaMes sanepar = new ContaFixaMes(sanepar(), YearMonth.of(2026, 11), new BigDecimal("80.00"));
        assertThat(sanepar.valorEfetivo()).isEqualByComparingTo("80.00");

        sanepar.registrarPagamento(new BigDecimal("89.43"), LocalDate.of(2026, 11, 17), null);

        assertThat(sanepar.valorEfetivo()).isEqualByComparingTo("89.43");
        assertThat(sanepar.situacao(LocalDate.of(2026, 11, 30))).isEqualTo(ContaFixaMes.Situacao.PAGA);
    }

    @Test
    @DisplayName("Dispensada não conta nada, e reabrir volta a ficar pendente")
    void dispensarEReabrir() {
        ContaFixaMes sanepar = new ContaFixaMes(sanepar(), YearMonth.of(2026, 11), new BigDecimal("80.00"));

        sanepar.dispensar();
        assertThat(sanepar.valorEfetivo()).isEqualByComparingTo("0");

        sanepar.reabrir();
        assertThat(sanepar.valorEfetivo()).isEqualByComparingTo("80.00");
        assertThat(sanepar.getStatus()).isEqualTo(StatusContaMes.PENDENTE);
    }

    @Test
    @DisplayName("O financiamento de R$ 558,30 fica R$ 279,15 para cada um dos pais")
    void partesDoFinanciamento() {
        DivisaoSalva pais = new DivisaoSalva("Pais 50/50", ModoDivisao.PERCENTUAL);
        pais.definirPartes(List.of(
                new DivisaoSalva.Integrante(marlene, new BigDecimal("50")),
                new DivisaoSalva.Integrante(nelson, new BigDecimal("50"))));

        ContaFixa financiamento = new ContaFixa("Financiamento Caixa", 17, YearMonth.of(2026, 1));
        financiamento.definirDestino(null, pais);

        List<ContaFixaMesResponse.Parte> partes = ContaFixaMesResponse.partes(financiamento, new BigDecimal("558.30"));

        assertThat(partes).extracting(ContaFixaMesResponse.Parte::valor)
                .containsExactly(new BigDecimal("279.15"), new BigDecimal("279.15"));
    }

    @Test
    @DisplayName("A chave de pagamento é guardada normalizada")
    void chaveNormalizada() {
        ContaFixa conta = sanepar();
        conta.definirChavePagamento("  Águas de  Sarandi ");

        assertThat(conta.getChavePagamento()).isEqualTo("AGUAS DE SARANDI");
    }

    private ContaFixa sanepar() {
        ContaFixa conta = new ContaFixa("Sanepar", 17, YearMonth.of(2026, 1));
        conta.definirDestino(marlene, null);
        return conta;
    }
}
