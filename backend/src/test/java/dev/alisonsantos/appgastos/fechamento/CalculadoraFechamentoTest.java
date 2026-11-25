package dev.alisonsantos.appgastos.fechamento;

import dev.alisonsantos.appgastos.cartao.Cartao;
import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.contafixa.ContaFixa;
import dev.alisonsantos.appgastos.contafixa.ContaFixaMes;
import dev.alisonsantos.appgastos.divisao.CalculadoraDivisao;
import dev.alisonsantos.appgastos.divisao.DivisaoSalva;
import dev.alisonsantos.appgastos.divisao.ModoDivisao;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.TipoPessoa;
import dev.alisonsantos.appgastos.transacao.OrigemPessoa;
import dev.alisonsantos.appgastos.transacao.StatusTransacao;
import dev.alisonsantos.appgastos.transacao.TipoMovimento;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do fechamento do mês, com um novembro parecido com o real.
 */
@DisplayName("Fechamento do mês")
class CalculadoraFechamentoTest {

    private static final YearMonth NOVEMBRO = YearMonth.of(2026, 11);

    private final CalculadoraFechamento calculadora = new CalculadoraFechamento();

    private final Conta contaCartao = new Conta("Santander", "Cartão Santander", TipoConta.CARTAO_CREDITO);
    private final Cartao cartao4948 = new Cartao(contaCartao, "4948");
    private final Cartao cartao3145 = new Cartao(contaCartao, "3145");

    private final Pessoa alison = new Pessoa("Alison", TipoPessoa.PESSOA);
    private final Pessoa gabriella = new Pessoa("Gabriella", TipoPessoa.PESSOA);
    private final Pessoa marlene = new Pessoa("Marlene", TipoPessoa.PESSOA);
    private final Pessoa nelson = new Pessoa("Nelson", TipoPessoa.PESSOA);

    @Test
    @DisplayName("Compra com dono vira uma linha para o dono")
    void compraComDono() {
        Transacao wellhub = compra("Wellhub Gabriella", "99.99", cartao4948);
        wellhub.definirPessoa(gabriella, OrigemPessoa.REGRA);

        List<LinhaFechamento> linhas = calculadora.montar(List.of(wellhub), List.of(), List.of());

        assertThat(linhas).singleElement().satisfies(l -> {
            assertThat(l.pessoa()).isSameAs(gabriella);
            assertThat(l.valor()).isEqualByComparingTo("99.99");
            assertThat(l.origem()).isEqualTo(OrigemItem.CARTAO);
            assertThat(l.detalhe()).isEqualTo("Cartão 4948");
        });
    }

    @Test
    @DisplayName("Compra dividida vira uma linha por pessoa, com a parte de cada uma")
    void compraDividida() {
        Transacao spotify = compra("Spotify Família", "40.90", cartao4948);
        spotify.aplicarDivisao(CalculadoraDivisao.calcular(ModoDivisao.IGUAL, List.of(
                new CalculadoraDivisao.Entrada(alison, null, null),
                new CalculadoraDivisao.Entrada(gabriella, null, null)), spotify.getValor()), OrigemPessoa.DIVISAO);

        List<LinhaFechamento> linhas = calculadora.montar(List.of(spotify), List.of(), List.of());

        assertThat(linhas).hasSize(2).allSatisfy(l -> {
            assertThat(l.dividida()).isTrue();
            assertThat(l.valor()).isEqualByComparingTo("20.45");
            assertThat(l.valorTotal()).isEqualByComparingTo("40.90");
        });
    }

    @Test
    @DisplayName("Financiamento no \"Pais 50/50\" vira R$ 279,15 para cada um")
    void contaFixaDividida() {
        List<LinhaFechamento> linhas = calculadora.montar(List.of(), List.of(), List.of(financiamento()));

        assertThat(linhas).extracting(LinhaFechamento::valor)
                .containsExactly(new BigDecimal("279.15"), new BigDecimal("279.15"));
        assertThat(linhas).allSatisfy(l -> assertThat(l.origem()).isEqualTo(OrigemItem.CONTA_FIXA));
    }

    @Test
    @DisplayName("Conta dispensada não entra no mês")
    void contaDispensada() {
        ContaFixaMes financiamento = financiamento();
        financiamento.dispensar();

        assertThat(calculadora.montar(List.of(), List.of(), List.of(financiamento))).isEmpty();
    }

    @Test
    @DisplayName("O total de cada pessoa soma cartões, conta corrente e contas fixas, com os estornos abatendo")
    void totaisPorPessoa() {
        Transacao mercado = compra("Atacadista Bem Bom", "150.00", cartao3145);
        mercado.definirPessoa(marlene, OrigemPessoa.CARTAO);
        Transacao estorno = compra("Estorno Atacadista", "-20.00", cartao3145);
        estorno.definirPessoa(marlene, OrigemPessoa.CARTAO);

        ContaFixa sanepar = new ContaFixa("Sanepar", 17, YearMonth.of(2026, 1));
        sanepar.definirDestino(marlene, null);
        ContaFixaMes saneparNovembro = new ContaFixaMes(sanepar, NOVEMBRO, null);
        saneparNovembro.registrarPagamento(new BigDecimal("89.43"), LocalDate.of(2026, 11, 17), null);

        List<LinhaFechamento> linhas = calculadora.montar(List.of(mercado, estorno), List.of(),
                List.of(saneparNovembro, financiamento()));
        List<TotalPessoa> totais = calculadora.totaisPorPessoa(linhas);

        TotalPessoa daMarlene = totais.stream().filter(t -> t.pessoa().equals("Marlene")).findFirst().orElseThrow();
        assertThat(daMarlene.cartoes()).isEqualByComparingTo("130.00");
        assertThat(daMarlene.contasFixas()).isEqualByComparingTo("368.58");
        assertThat(daMarlene.total()).isEqualByComparingTo("498.58");
        assertThat(daMarlene.itens()).isEqualTo(4);
    }

    @Test
    @DisplayName("Compra sem dono fica de fora dos totais e aparece no grupo \"sem dono\"")
    void semDono() {
        Transacao uber = compra("DL*UberRides", "23.10", cartao4948);
        Transacao comDono = compra("Padaria", "10.00", cartao4948);
        comDono.definirPessoa(alison, OrigemPessoa.CARTAO);

        List<LinhaFechamento> linhas = calculadora.montar(List.of(uber, comDono), List.of(), List.of());

        assertThat(calculadora.totaisPorPessoa(linhas)).singleElement()
                .satisfies(t -> assertThat(t.pessoa()).isEqualTo("Alison"));
        assertThat(calculadora.totalSemDono(linhas)).satisfies(t -> {
            assertThat(t.total()).isEqualByComparingTo("23.10");
            assertThat(t.itens()).isEqualTo(1);
        });
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Transacao compra(String descricao, String valor, Cartao cartao) {
        Transacao t = new Transacao("pluggy-" + descricao.hashCode(), contaCartao, Instant.parse("2026-10-20T15:00:00Z"),
                new BigDecimal(valor), descricao,
                valor.startsWith("-") ? TipoMovimento.CREDITO : TipoMovimento.DEBITO, StatusTransacao.CONFIRMADA);
        t.setCartao(cartao);
        return t;
    }

    private ContaFixaMes financiamento() {
        DivisaoSalva pais = new DivisaoSalva("Pais 50/50", ModoDivisao.PERCENTUAL);
        pais.definirPartes(List.of(
                new DivisaoSalva.Integrante(marlene, new BigDecimal("50")),
                new DivisaoSalva.Integrante(nelson, new BigDecimal("50"))));
        ContaFixa conta = new ContaFixa("Financiamento Caixa", 17, YearMonth.of(2026, 1));
        conta.definirDestino(null, pais);
        conta.setValorEsperado(new BigDecimal("558.30"));
        return new ContaFixaMes(conta, NOVEMBRO, new BigDecimal("558.30"));
    }
}
