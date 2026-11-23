package dev.alisonsantos.appgastos.divisao;

import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.pessoa.Pessoa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Calcula os valores de uma divisão, sem perder nenhum centavo.
 *
 * O PROBLEMA: R$ 40,90 dividido por 3 dá 13,6333... Arredondando cada
 * parte para 13,63, a soma dá 40,89, e some 1 centavo.
 *
 * A SOLUÇÃO (método do "maior resto"): cada um recebe a parte cortada
 * para baixo (13,63), e os centavos que sobram vão, um a um, para quem
 * teve o maior pedaço cortado. Resultado: 13,64 + 13,63 + 13,63 = 40,90.
 *
 * Só funções puras, sem banco: fácil de testar.
 */
public final class CalculadoraDivisao {

    private static final int ESCALA_FRACAO = 10;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal TOLERANCIA_PERCENTUAL = new BigDecimal("0.01");

    private CalculadoraDivisao() {
    }

    /** O que vem do pedido para cada pessoa (valor ou percentual, conforme o modo). */
    public record Entrada(Pessoa pessoa, BigDecimal valor, BigDecimal percentual) {
    }

    /**
     * Calcula a divisão de um total.
     *
     * @throws RegraNegocioException se os dados não fecharem (menos de 2 pessoas,
     *                               pessoa repetida, soma diferente do total...)
     */
    public static List<ParteCalculada> calcular(ModoDivisao modo, List<Entrada> entradas, BigDecimal total) {
        validarPessoas(entradas);
        if (total.signum() == 0) {
            throw new RegraNegocioException("Não dá para dividir uma transação de valor zero.");
        }

        return switch (modo) {
            case IGUAL -> distribuir(total, pessoas(entradas), fracoesIguais(entradas.size()));
            case PERCENTUAL -> distribuir(total, pessoas(entradas), fracoesDosPercentuais(entradas));
            case VALOR -> partesPorValor(entradas, total);
        };
    }

    /**
     * Aplica as MESMAS proporções de uma divisão a outro valor.
     * Usado nas outras parcelas da compra (que podem ter valor diferente
     * por causa de centavos) e nas parcelas futuras.
     */
    public static List<ParteCalculada> mesmasProporcoes(List<Rateio> rateios, BigDecimal novoTotal) {
        return distribuir(novoTotal,
                rateios.stream().map(Rateio::getPessoa).toList(),
                rateios.stream().map(Rateio::getFracao).toList());
    }

    /** Mesma ideia, a partir de partes calculadas (antes de virarem rateios). */
    public static List<ParteCalculada> redistribuir(List<ParteCalculada> partes, BigDecimal novoTotal) {
        return distribuir(novoTotal,
                partes.stream().map(ParteCalculada::pessoa).toList(),
                partes.stream().map(ParteCalculada::fracao).toList());
    }

    // ------------------------------------------------------------------
    // O método do maior resto
    // ------------------------------------------------------------------

    static List<ParteCalculada> distribuir(BigDecimal total, List<Pessoa> pessoas, List<BigDecimal> fracoes) {
        int sinal = total.signum();
        long centavosTotais = total.abs().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();

        long[] centavos = new long[pessoas.size()];
        BigDecimal[] restos = new BigDecimal[pessoas.size()];
        long distribuidos = 0;

        for (int i = 0; i < pessoas.size(); i++) {
            BigDecimal exato = BigDecimal.valueOf(centavosTotais).multiply(fracoes.get(i));
            centavos[i] = exato.setScale(0, RoundingMode.DOWN).longValueExact();
            restos[i] = exato.subtract(BigDecimal.valueOf(centavos[i]));
            distribuidos += centavos[i];
        }

        // Os centavos que sobraram vão para quem teve o maior resto (empate: a ordem do pedido)
        long sobra = centavosTotais - distribuidos;
        List<Integer> porMaiorResto = IntStream.range(0, pessoas.size()).boxed()
                .sorted(Comparator.comparing((Integer i) -> restos[i]).reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
        for (int k = 0; k < sobra; k++) {
            centavos[porMaiorResto.get(k % porMaiorResto.size())]++;
        }
        // Caso raro (frações que somam um pouco mais de 1 por arredondamento):
        // tira o centavo excedente de quem teve o menor resto
        for (int k = 0; k < -sobra; k++) {
            centavos[porMaiorResto.get(porMaiorResto.size() - 1 - (k % porMaiorResto.size()))]--;
        }

        List<ParteCalculada> partes = new ArrayList<>();
        for (int i = 0; i < pessoas.size(); i++) {
            BigDecimal valor = BigDecimal.valueOf(centavos[i] * sinal).movePointLeft(2);
            partes.add(new ParteCalculada(pessoas.get(i), valor, fracoes.get(i)));
        }
        return partes;
    }

    // ------------------------------------------------------------------
    // Os modos
    // ------------------------------------------------------------------

    private static List<BigDecimal> fracoesIguais(int quantidade) {
        BigDecimal fracao = BigDecimal.ONE.divide(BigDecimal.valueOf(quantidade), ESCALA_FRACAO, RoundingMode.HALF_UP);
        return IntStream.range(0, quantidade).mapToObj(i -> fracao).toList();
    }

    private static List<BigDecimal> fracoesDosPercentuais(List<Entrada> entradas) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Entrada entrada : entradas) {
            if (entrada.percentual() == null || entrada.percentual().signum() <= 0) {
                throw new RegraNegocioException("No modo PERCENTUAL, informe um percentual maior que zero para cada pessoa.");
            }
            soma = soma.add(entrada.percentual());
        }
        if (soma.subtract(CEM).abs().compareTo(TOLERANCIA_PERCENTUAL) > 0) {
            throw new RegraNegocioException("Os percentuais somam " + soma.stripTrailingZeros().toPlainString()
                    + "%, mas precisam somar 100%.");
        }
        // Cópia "final" da soma: variáveis usadas dentro de uma lambda não
        // podem ser alteradas depois de criadas (e a "soma" foi, no laço acima)
        BigDecimal somaTotal = soma;
        return entradas.stream()
                .map(e -> e.percentual().divide(somaTotal, ESCALA_FRACAO, RoundingMode.HALF_UP))
                .toList();
    }

    /** No modo VALOR, os valores informados são usados exatamente como vieram. */
    private static List<ParteCalculada> partesPorValor(List<Entrada> entradas, BigDecimal total) {
        BigDecimal soma = BigDecimal.ZERO;
        for (Entrada entrada : entradas) {
            if (entrada.valor() == null || entrada.valor().signum() <= 0) {
                throw new RegraNegocioException("No modo VALOR, informe um valor maior que zero para cada pessoa.");
            }
            soma = soma.add(entrada.valor());
        }
        BigDecimal totalPositivo = total.abs();
        if (soma.compareTo(totalPositivo) != 0) {
            throw new RegraNegocioException("Os valores somam R$ " + soma.toPlainString()
                    + ", mas a transação é de R$ " + totalPositivo.toPlainString() + ".");
        }

        // Estorno (valor negativo): as partes também ficam negativas
        BigDecimal sinal = BigDecimal.valueOf(total.signum());
        return entradas.stream()
                .map(e -> new ParteCalculada(
                        e.pessoa(),
                        e.valor().multiply(sinal).setScale(2, RoundingMode.HALF_UP),
                        e.valor().divide(totalPositivo, ESCALA_FRACAO, RoundingMode.HALF_UP)))
                .toList();
    }

    // ------------------------------------------------------------------
    // Validações
    // ------------------------------------------------------------------

    private static void validarPessoas(List<Entrada> entradas) {
        if (entradas == null || entradas.size() < 2) {
            throw new RegraNegocioException("Para dividir, informe pelo menos 2 pessoas.");
        }
        Set<Pessoa> vistas = new HashSet<>();
        for (Entrada entrada : entradas) {
            if (!vistas.add(entrada.pessoa())) {
                throw new RegraNegocioException("A pessoa \"" + entrada.pessoa().getNome() + "\" aparece mais de uma vez na divisão.");
            }
        }
    }

    private static List<Pessoa> pessoas(List<Entrada> entradas) {
        return entradas.stream().map(Entrada::pessoa).toList();
    }
}
