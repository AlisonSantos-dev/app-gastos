package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.comum.Texto;
import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Encontra, no extrato, o pagamento de cada conta fixa pendente.
 *
 * Uma transação é o pagamento de uma conta quando:
 *   1. é uma saída (valor negativo);
 *   2. saiu da conta corrente de onde a conta costuma ser paga;
 *   3. a descrição do extrato contém a chave da conta (ex.: "SANEPAR");
 *   4. aconteceu até 20 dias antes ou depois do vencimento.
 *
 * Se houver mais de uma, vence a mais próxima do vencimento. E uma
 * transação paga uma conta só.
 *
 * A janela de 20 dias evita confundir meses: o pagamento de 16/10 está a
 * 32 dias do vencimento de 17/11, então não é tomado como o de novembro.
 *
 * Não acessa banco: recebe tudo pronto. Por isso é fácil de testar.
 */
@Component
public class ConciliadorPagamentos {

    static final int JANELA_DIAS = 20;
    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");

    /** Uma conta do mês e o pagamento encontrado para ela. */
    public record Vinculo(ContaFixaMes conta, Transacao pagamento) {
    }

    public List<Vinculo> conciliar(List<ContaFixaMes> pendentes, List<Transacao> candidatos) {
        Set<Transacao> usadas = new HashSet<>();
        List<Vinculo> vinculos = new ArrayList<>();

        // Ordem fixa (vencimento, depois nome), para o resultado ser sempre o mesmo
        List<ContaFixaMes> ordenadas = pendentes.stream()
                .filter(mes -> mes.getStatus() == StatusContaMes.PENDENTE)
                .filter(mes -> mes.getContaFixa().getChavePagamento() != null)
                .sorted(Comparator.comparing(ContaFixaMes::getVencimento)
                        .thenComparing(mes -> mes.getContaFixa().getNome()))
                .toList();

        for (ContaFixaMes mes : ordenadas) {
            melhorPagamento(mes, candidatos, usadas).ifPresent(pagamento -> {
                usadas.add(pagamento);
                vinculos.add(new Vinculo(mes, pagamento));
            });
        }
        return vinculos;
    }

    private Optional<Transacao> melhorPagamento(ContaFixaMes mes, List<Transacao> candidatos, Set<Transacao> usadas) {
        ContaFixa conta = mes.getContaFixa();
        String chave = conta.getChavePagamento();
        LocalDate vencimento = mes.getVencimento();

        return candidatos.stream()
                .filter(t -> !usadas.contains(t))
                .filter(t -> t.getValor().signum() < 0)
                .filter(t -> conta.getConta() == null || mesmaConta(conta.getConta(), t.getConta()))
                .filter(t -> Texto.normalizar(t.getDescricaoBanco()).contains(chave))
                .filter(t -> diasAteVencimento(t, vencimento) <= JANELA_DIAS)
                .min(Comparator.comparingLong((Transacao t) -> diasAteVencimento(t, vencimento))
                        .thenComparing(Transacao::getDataTransacao));
    }

    private long diasAteVencimento(Transacao t, LocalDate vencimento) {
        return Math.abs(ChronoUnit.DAYS.between(data(t), vencimento));
    }

    static LocalDate data(Transacao t) {
        return LocalDate.ofInstant(t.getDataTransacao(), FUSO_BRASIL);
    }

    /** Pelo id (no app) ou pela própria instância (nos testes, sem id). */
    private boolean mesmaConta(Conta a, Conta b) {
        return a == b || (a.getId() != null && a.getId().equals(b.getId()));
    }
}
