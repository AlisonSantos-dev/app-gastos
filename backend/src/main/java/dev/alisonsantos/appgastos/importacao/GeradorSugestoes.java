package dev.alisonsantos.appgastos.importacao;

import dev.alisonsantos.appgastos.comum.Texto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transforma as linhas da planilha em sugestões de regras.
 *
 * 1. Agrupa as linhas pela descrição do fornecedor (normalizada).
 * 2. Conta COMPRAS, e não linhas: uma compra em 12x aparece em 12 abas,
 *    mas é uma compra só. Sem isso, uma única compra parcelada na Magalu
 *    viraria a regra "toda compra na Magalu é a máquina de lavar".
 * 3. Só sugere descrições usadas em pelo menos 2 compras diferentes,
 *    com o mesmo nome em pelo menos 60% delas.
 * 4. Calcula o impacto: quantas transações do app, hoje sem regra,
 *    a sugestão identificaria.
 *
 * Não acessa banco nem arquivo: recebe tudo pronto. Por isso é testável.
 */
@Component
public class GeradorSugestoes {

    static final int MINIMO_COMPRAS = 2;
    static final double CONFIANCA_MINIMA = 0.6;
    static final int TAMANHO_MINIMO_CHAVE = 3;

    /**
     * @param linhas               as compras lidas da planilha
     * @param descricoesSemRegra   as descrições do banco (já normalizadas) das transações sem regra
     */
    public List<SugestaoCalculada> gerar(List<LinhaPlanilha> linhas, List<String> descricoesSemRegra) {
        Map<String, List<LinhaPlanilha>> porFornecedor = linhas.stream()
                .collect(Collectors.groupingBy(l -> Texto.normalizar(l.descricaoFornecedor()),
                        LinkedHashMap::new, Collectors.toList()));

        List<SugestaoCalculada> sugestoes = new ArrayList<>();
        porFornecedor.forEach((chave, linhasDoFornecedor) -> {
            if (chave.length() < TAMANHO_MINIMO_CHAVE) {
                return;
            }
            List<LinhaPlanilha> compras = comprasDistintas(linhasDoFornecedor);
            if (compras.size() < MINIMO_COMPRAS) {
                return;
            }

            // O nome mais usado (comparando normalizado, mas mostrando como você escreveu)
            Map.Entry<String, Long> nomeMaisUsado = maisFrequente(compras, l -> Texto.normalizar(l.descricaoAmigavel()));
            double confiancaDescricao = nomeMaisUsado.getValue() / (double) compras.size();
            if (confiancaDescricao < CONFIANCA_MINIMA) {
                return;
            }
            String descricao = compras.stream()
                    .filter(l -> Texto.normalizar(l.descricaoAmigavel()).equals(nomeMaisUsado.getKey()))
                    .map(LinhaPlanilha::descricaoAmigavel)
                    .findFirst()
                    .orElse(nomeMaisUsado.getKey());

            Map.Entry<SecaoPlanilha, Long> secaoMaisUsada = maisFrequente(compras, LinhaPlanilha::secao);

            int afetadas = (int) descricoesSemRegra.stream().filter(d -> d.contains(chave)).count();

            LocalDate ultima = compras.stream()
                    .map(LinhaPlanilha::dataCompra)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            sugestoes.add(new SugestaoCalculada(
                    chave,
                    descricao,
                    compras.size(),
                    arredondar(confiancaDescricao),
                    secaoMaisUsada.getKey(),
                    arredondar(secaoMaisUsada.getValue() / (double) compras.size()),
                    afetadas,
                    ultima));
        });

        // As que mais ajudam primeiro: as que identificam mais transações do app
        sugestoes.sort(Comparator.comparingInt(SugestaoCalculada::transacoesAfetadas).reversed()
                .thenComparing(Comparator.comparingInt(SugestaoCalculada::compras).reversed()));
        return sugestoes;
    }

    /**
     * Uma compra = uma data de compra. As parcelas da mesma compra (mesma
     * data, em abas diferentes) contam uma vez só. Sem data, cada aba conta.
     */
    private List<LinhaPlanilha> comprasDistintas(List<LinhaPlanilha> linhas) {
        Map<String, LinhaPlanilha> unicas = new LinkedHashMap<>();
        for (LinhaPlanilha linha : linhas) {
            String chave = linha.dataCompra() != null
                    ? linha.dataCompra().toString()
                    : linha.aba() + "|" + Texto.normalizar(linha.descricaoAmigavel());
            unicas.putIfAbsent(chave, linha);
        }
        return new ArrayList<>(unicas.values());
    }

    private <K> Map.Entry<K, Long> maisFrequente(List<LinhaPlanilha> linhas, Function<LinhaPlanilha, K> criterio) {
        return linhas.stream()
                .collect(Collectors.groupingBy(criterio, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
