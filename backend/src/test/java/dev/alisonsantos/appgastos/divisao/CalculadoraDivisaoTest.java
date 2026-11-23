package dev.alisonsantos.appgastos.divisao;

import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.TipoPessoa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes da divisão de valores, com casos da planilha real.
 */
@DisplayName("Divisão de compras")
class CalculadoraDivisaoTest {

    private final Pessoa alison = pessoa("Alison");
    private final Pessoa gabriella = pessoa("Gabriella");
    private final Pessoa marlene = pessoa("Marlene");
    private final Pessoa nelson = pessoa("Nelson");
    private final Pessoa paulo = pessoa("Paulo");

    @Test
    @DisplayName("Spotify Família: R$ 40,90 em 5 partes iguais dá R$ 8,18 para cada")
    void spotifyFamilia() {
        List<ParteCalculada> partes = CalculadoraDivisao.calcular(ModoDivisao.IGUAL,
                entradas(alison, gabriella, marlene, nelson, paulo), dinheiro("40.90"));

        assertThat(partes).extracting(ParteCalculada::valor)
                .containsOnly(dinheiro("8.18"));
    }

    @Test
    @DisplayName("R$ 40,90 em 3 partes: o centavo que sobra vai para alguém, e nada se perde")
    void centavoQueSobra() {
        List<ParteCalculada> partes = CalculadoraDivisao.calcular(ModoDivisao.IGUAL,
                entradas(alison, gabriella, marlene), dinheiro("40.90"));

        assertThat(partes).extracting(ParteCalculada::valor)
                .containsExactly(dinheiro("13.64"), dinheiro("13.63"), dinheiro("13.63"));
        assertThat(soma(partes)).isEqualByComparingTo("40.90");
    }

    @Test
    @DisplayName("Financiamento 50% / 50% entre a Marlene e o Nelson")
    void financiamentoMeioAMeio() {
        List<ParteCalculada> partes = CalculadoraDivisao.calcular(ModoDivisao.PERCENTUAL,
                List.of(new CalculadoraDivisao.Entrada(marlene, null, new BigDecimal("50")),
                        new CalculadoraDivisao.Entrada(nelson, null, new BigDecimal("50"))),
                dinheiro("558.30"));

        assertThat(partes).extracting(ParteCalculada::valor)
                .containsExactly(dinheiro("279.15"), dinheiro("279.15"));
    }

    @Test
    @DisplayName("Por valor: usa exatamente os valores informados")
    void porValor() {
        List<ParteCalculada> partes = CalculadoraDivisao.calcular(ModoDivisao.VALOR,
                List.of(new CalculadoraDivisao.Entrada(alison, dinheiro("60.00"), null),
                        new CalculadoraDivisao.Entrada(gabriella, dinheiro("40.00"), null)),
                dinheiro("100.00"));

        assertThat(partes).extracting(ParteCalculada::valor)
                .containsExactly(dinheiro("60.00"), dinheiro("40.00"));
        assertThat(partes.getFirst().fracao()).isEqualByComparingTo("0.6");
    }

    @Test
    @DisplayName("Estorno (valor negativo) também é dividido, com as partes negativas")
    void estorno() {
        List<ParteCalculada> partes = CalculadoraDivisao.calcular(ModoDivisao.IGUAL,
                entradas(alison, gabriella), dinheiro("-17.93"));

        assertThat(partes).extracting(ParteCalculada::valor)
                .containsExactly(dinheiro("-8.97"), dinheiro("-8.96"));
    }

    @Test
    @DisplayName("As outras parcelas recebem as mesmas proporções, sobre o valor delas")
    void mesmasProporcoesEmOutraParcela() {
        List<ParteCalculada> primeiraParcela = CalculadoraDivisao.calcular(ModoDivisao.IGUAL,
                entradas(alison, gabriella, marlene), dinheiro("40.90"));

        List<ParteCalculada> outraParcela = CalculadoraDivisao.redistribuir(primeiraParcela, dinheiro("30.00"));

        assertThat(outraParcela).extracting(ParteCalculada::valor)
                .containsOnly(dinheiro("10.00"));
    }

    @Test
    @DisplayName("Valores que não somam o total são recusados")
    void valoresQueNaoFecham() {
        assertThatThrownBy(() -> CalculadoraDivisao.calcular(ModoDivisao.VALOR,
                List.of(new CalculadoraDivisao.Entrada(alison, dinheiro("60.00"), null),
                        new CalculadoraDivisao.Entrada(gabriella, dinheiro("30.00"), null)),
                dinheiro("100.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("somam R$ 90.00");
    }

    @Test
    @DisplayName("Percentuais que não somam 100 são recusados")
    void percentuaisQueNaoFecham() {
        assertThatThrownBy(() -> CalculadoraDivisao.calcular(ModoDivisao.PERCENTUAL,
                List.of(new CalculadoraDivisao.Entrada(marlene, null, new BigDecimal("50")),
                        new CalculadoraDivisao.Entrada(nelson, null, new BigDecimal("40"))),
                dinheiro("558.30")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("90%");
    }

    @Test
    @DisplayName("A mesma pessoa duas vezes, ou uma pessoa só, é recusado")
    void pessoasInvalidas() {
        assertThatThrownBy(() -> CalculadoraDivisao.calcular(ModoDivisao.IGUAL,
                entradas(alison, alison), dinheiro("10.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("mais de uma vez");

        assertThatThrownBy(() -> CalculadoraDivisao.calcular(ModoDivisao.IGUAL,
                entradas(alison), dinheiro("10.00")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("pelo menos 2");
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private static Pessoa pessoa(String nome) {
        return new Pessoa(nome, TipoPessoa.PESSOA);
    }

    private static BigDecimal dinheiro(String valor) {
        return new BigDecimal(valor);
    }

    private static List<CalculadoraDivisao.Entrada> entradas(Pessoa... pessoas) {
        return java.util.Arrays.stream(pessoas)
                .map(p -> new CalculadoraDivisao.Entrada(p, null, null))
                .toList();
    }

    private static BigDecimal soma(List<ParteCalculada> partes) {
        return partes.stream().map(ParteCalculada::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
