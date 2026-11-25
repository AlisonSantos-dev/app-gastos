package dev.alisonsantos.appgastos.fechamento;

import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.contafixa.ContaFixa;
import dev.alisonsantos.appgastos.contafixa.ContaFixaMes;
import dev.alisonsantos.appgastos.divisao.CalculadoraDivisao;
import dev.alisonsantos.appgastos.divisao.ParteCalculada;
import dev.alisonsantos.appgastos.divisao.Rateio;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforma as compras e as contas do mês em linhas por pessoa, e as
 * linhas em totais.
 *
 *   - Compra com dono:      1 linha, para o dono.
 *   - Compra dividida:      1 linha por pessoa, com a parte de cada uma.
 *   - Compra sem dono:      1 linha "sem dono".
 *   - Conta fixa:           1 linha para a pessoa da conta, ou 1 por pessoa da divisão.
 *
 * Não acessa banco: recebe tudo pronto. Por isso é fácil de testar.
 */
@Component
public class CalculadoraFechamento {

    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");

    /** Monta todas as linhas do mês. */
    public List<LinhaFechamento> montar(List<Transacao> comprasCartao, List<Transacao> gastosContaCorrente,
                                        List<ContaFixaMes> contasFixas) {
        List<LinhaFechamento> linhas = new ArrayList<>();
        comprasCartao.forEach(t -> linhas.addAll(linhasDaTransacao(t, OrigemItem.CARTAO)));
        gastosContaCorrente.forEach(t -> linhas.addAll(linhasDaTransacao(t, OrigemItem.CONTA_CORRENTE)));
        contasFixas.forEach(c -> linhas.addAll(linhasDaContaFixa(c)));
        return linhas;
    }

    List<LinhaFechamento> linhasDaTransacao(Transacao t, OrigemItem origem) {
        LocalDate data = LocalDate.ofInstant(t.getDataTransacao(), FUSO_BRASIL);
        String detalhe = detalhe(t, origem);

        if (t.isDividida()) {
            List<LinhaFechamento> partes = new ArrayList<>();
            for (Rateio rateio : t.getRateios()) {
                partes.add(new LinhaFechamento(rateio.getPessoa(), origem, t.getId(), null, data,
                        t.getDescricaoExibicao(), detalhe, rateio.getValor(), t.getValor(), true));
            }
            return partes;
        }
        return List.of(new LinhaFechamento(t.getPessoa(), origem, t.getId(), null, data,
                t.getDescricaoExibicao(), detalhe, t.getValor(), t.getValor(), false));
    }

    List<LinhaFechamento> linhasDaContaFixa(ContaFixaMes mes) {
        BigDecimal valor = mes.valorEfetivo();
        if (valor == null || valor.signum() == 0) {
            return List.of();
        }
        ContaFixa conta = mes.getContaFixa();
        LocalDate data = mes.getDataPagamento() != null ? mes.getDataPagamento() : mes.getVencimento();
        String detalhe = conta.getGrupo();

        if (conta.getPessoa() != null) {
            return List.of(new LinhaFechamento(conta.getPessoa(), OrigemItem.CONTA_FIXA, null, mes.getId(), data,
                    conta.getNome(), detalhe, valor, valor, false));
        }

        List<ParteCalculada> partes;
        try {
            partes = CalculadoraDivisao.calcular(conta.getDivisaoSalva().getModo(),
                    conta.getDivisaoSalva().paraEntradas(), valor);
        } catch (RegraNegocioException e) {
            // Divisão inválida (ex.: todos desativados): a conta fica sem dono, e o alerta aparece
            return List.of(new LinhaFechamento(null, OrigemItem.CONTA_FIXA, null, mes.getId(), data,
                    conta.getNome(), detalhe, valor, valor, false));
        }
        return partes.stream()
                .map(p -> new LinhaFechamento(p.pessoa(), OrigemItem.CONTA_FIXA, null, mes.getId(), data,
                        conta.getNome(), detalhe, p.valor(), valor, true))
                .toList();
    }

    /**
     * Soma as linhas por pessoa. O grupo "sem dono" (pessoaId null) fica
     * de fora desta lista: ele é mostrado à parte, como alerta.
     */
    public List<TotalPessoa> totaisPorPessoa(List<LinhaFechamento> linhas) {
        Map<Long, List<LinhaFechamento>> porPessoa = new LinkedHashMap<>();
        for (LinhaFechamento linha : linhas) {
            if (linha.pessoa() != null) {
                porPessoa.computeIfAbsent(chave(linha.pessoa()), k -> new ArrayList<>()).add(linha);
            }
        }
        return porPessoa.values().stream()
                .map(this::totalizar)
                .sorted(Comparator.comparing(TotalPessoa::total).reversed())
                .toList();
    }

    /** O total das linhas sem dono (null se não houver nenhuma). */
    public TotalPessoa totalSemDono(List<LinhaFechamento> linhas) {
        List<LinhaFechamento> semDono = linhas.stream().filter(l -> l.pessoa() == null).toList();
        return semDono.isEmpty() ? null : totalizar(semDono);
    }

    private TotalPessoa totalizar(List<LinhaFechamento> linhas) {
        Pessoa pessoa = linhas.getFirst().pessoa();
        return new TotalPessoa(
                pessoa == null ? null : pessoa.getId(),
                pessoa == null ? "Sem dono" : pessoa.getNome(),
                pessoa == null ? null : pessoa.getCor(),
                somar(linhas, null),
                somar(linhas, OrigemItem.CARTAO),
                somar(linhas, OrigemItem.CONTA_CORRENTE),
                somar(linhas, OrigemItem.CONTA_FIXA),
                linhas.size());
    }

    private BigDecimal somar(List<LinhaFechamento> linhas, OrigemItem origem) {
        return linhas.stream()
                .filter(l -> origem == null || l.origem() == origem)
                .map(LinhaFechamento::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String detalhe(Transacao t, OrigemItem origem) {
        if (origem == OrigemItem.CARTAO && t.getCartao() != null) {
            String parcela = t.getParcelaTotal() == null ? "" : " · " + t.getParcelaNumero() + "/" + t.getParcelaTotal();
            return "Cartão " + t.getCartao().getUltimosDigitos() + parcela;
        }
        return t.getConta().getNome();
    }

    /** Pelo id; nos testes (sem id), um id negativo único por instância. */
    private Long chave(Pessoa pessoa) {
        return pessoa.getId() != null ? pessoa.getId() : -(long) System.identityHashCode(pessoa);
    }
}
