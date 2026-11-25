package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.TipoPessoa;
import dev.alisonsantos.appgastos.transacao.StatusTransacao;
import dev.alisonsantos.appgastos.transacao.TipoMovimento;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes da busca dos pagamentos das contas fixas, com os extratos reais.
 */
@DisplayName("Contas fixas: pagamentos no extrato")
class ConciliadorPagamentosTest {

    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);

    private final ConciliadorPagamentos conciliador = new ConciliadorPagamentos();

    private final Conta santander = new Conta("Santander", "Conta Santander", TipoConta.CORRENTE);
    private final Conta itau = new Conta("Itaú", "Conta Itaú", TipoConta.CORRENTE);
    private final Pessoa marlene = new Pessoa("Marlene", TipoPessoa.PESSOA);

    @Test
    @DisplayName("Acha a Sanepar paga na conta Santander, no dia do vencimento")
    void achaSanepar() {
        ContaFixaMes sanepar = contaDoMes("Sanepar", "SANEPAR", santander, 17);
        Transacao pagamento = saida(santander, "CONTA DE AGUA E ESGOTO EM CANAIS   INTERNET SANEPAR PARANA",
                "-89.43", "2026-11-17");

        List<ConciliadorPagamentos.Vinculo> vinculos = conciliador.conciliar(List.of(sanepar), List.of(pagamento));

        assertThat(vinculos).singleElement().satisfies(v -> {
            assertThat(v.conta()).isSameAs(sanepar);
            assertThat(v.pagamento()).isSameAs(pagamento);
        });
    }

    @Test
    @DisplayName("Acha a Copel paga por Pix QR Code no Itaú, um dia antes do vencimento")
    void achaCopelPorPix() {
        ContaFixaMes copel = contaDoMes("Copel", "COPEL", itau, 10);
        Transacao pix = saida(itau, "Pagamento de Pix QR Code COPEL-DIS", "-181.73", "2026-11-09");

        assertThat(conciliador.conciliar(List.of(copel), List.of(pix))).hasSize(1);
    }

    @Test
    @DisplayName("O pagamento do mês anterior (32 dias antes) não é tomado como o deste mês")
    void naoConfundeMeses() {
        ContaFixaMes sanepar = contaDoMes("Sanepar", "SANEPAR", santander, 17);
        Transacao deOutubro = saida(santander, "CONTA DE AGUA E ESGOTO SANEPAR", "-85.10", "2026-10-16");

        assertThat(conciliador.conciliar(List.of(sanepar), List.of(deOutubro))).isEmpty();
    }

    @Test
    @DisplayName("Saída de outra conta corrente não é o pagamento")
    void outraConta() {
        ContaFixaMes copel = contaDoMes("Copel", "COPEL", itau, 10);
        Transacao naSantander = saida(santander, "PAGAMENTO COPEL", "-181.73", "2026-11-09");

        assertThat(conciliador.conciliar(List.of(copel), List.of(naSantander))).isEmpty();
    }

    @Test
    @DisplayName("Entre dois candidatos, vence o mais próximo do vencimento")
    void maisProximo() {
        ContaFixaMes sanepar = contaDoMes("Sanepar", "SANEPAR", santander, 17);
        Transacao longe = saida(santander, "SANEPAR", "-12.00", "2026-11-02");
        Transacao perto = saida(santander, "SANEPAR", "-89.43", "2026-11-16");

        assertThat(conciliador.conciliar(List.of(sanepar), List.of(longe, perto)))
                .singleElement()
                .satisfies(v -> assertThat(v.pagamento()).isSameAs(perto));
    }

    @Test
    @DisplayName("Uma transação paga uma conta só")
    void umaTransacaoUmaConta() {
        ContaFixaMes conta1 = contaDoMes("Sanepar casa 1", "SANEPAR", santander, 17);
        ContaFixaMes conta2 = contaDoMes("Sanepar casa 2", "SANEPAR", santander, 17);
        Transacao pagamento = saida(santander, "SANEPAR", "-89.43", "2026-11-17");

        assertThat(conciliador.conciliar(List.of(conta1, conta2), List.of(pagamento))).hasSize(1);
    }

    @Test
    @DisplayName("Conta já paga (ou dispensada) não procura pagamento")
    void jaPaga() {
        ContaFixaMes sanepar = contaDoMes("Sanepar", "SANEPAR", santander, 17);
        sanepar.dispensar();
        Transacao pagamento = saida(santander, "SANEPAR", "-89.43", "2026-11-17");

        assertThat(conciliador.conciliar(List.of(sanepar), List.of(pagamento))).isEmpty();
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private ContaFixaMes contaDoMes(String nome, String chave, Conta contaCorrente, int dia) {
        ContaFixa conta = new ContaFixa(nome, dia, YearMonth.of(2026, 1));
        conta.setConta(contaCorrente);
        conta.definirChavePagamento(chave);
        conta.definirDestino(marlene, null);
        return new ContaFixaMes(conta, NOVEMBRO, null);
    }

    /** Uma saída da conta corrente, ao meio-dia (horário de Brasília) do dia informado. */
    private Transacao saida(Conta conta, String descricao, String valor, String dia) {
        return new Transacao("pluggy-" + descricao.hashCode() + dia, conta,
                Instant.parse(dia + "T15:00:00Z"), new BigDecimal(valor), descricao,
                TipoMovimento.DEBITO, StatusTransacao.CONFIRMADA);
    }
}
