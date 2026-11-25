package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * As contas fixas de cada mês.
 *
 * "Gerar" o mês cria a conta do mês para cada conta fixa vigente que
 * ainda não a tenha. Pode ser chamado quantas vezes quiser: o que já
 * existe não é duplicado (é idempotente).
 */
@Service
public class ContaFixaMesService {

    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");

    private final ContaFixaRepository contaFixaRepository;
    private final ContaFixaMesRepository mesRepository;

    public ContaFixaMesService(ContaFixaRepository contaFixaRepository, ContaFixaMesRepository mesRepository) {
        this.contaFixaRepository = contaFixaRepository;
        this.mesRepository = mesRepository;
    }

    /** Cria as contas do mês que ainda não existem e devolve o mês completo. */
    @Transactional
    public ResumoMesResponse gerar(YearMonth competencia) {
        Set<Long> jaGeradas = mesRepository.findByCompetencia(competencia.atDay(1)).stream()
                .map(mes -> mes.getContaFixa().getId())
                .collect(Collectors.toSet());

        List<ContaFixaMes> novas = contaFixaRepository.findByAtivaTrue().stream()
                .filter(conta -> conta.isVigenteEm(competencia))
                .filter(conta -> !jaGeradas.contains(conta.getId()))
                .map(conta -> new ContaFixaMes(conta, competencia, previsao(conta)))
                .toList();
        mesRepository.saveAll(novas);

        return resumo(competencia);
    }

    @Transactional(readOnly = true)
    public ResumoMesResponse listar(YearMonth competencia) {
        return resumo(competencia);
    }

    /** Ajustes manuais: pagar fora do extrato, ajustar a previsão, dispensar, reabrir. */
    @Transactional
    public ContaFixaMesResponse atualizar(Long id, LancamentoContaRequest pedido) {
        ContaFixaMes mes = mesRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Conta do mês", id));

        boolean dispensar = Boolean.TRUE.equals(pedido.dispensar());
        boolean reabrir = Boolean.TRUE.equals(pedido.reabrir());
        if (dispensar && reabrir) {
            throw new RegraNegocioException("Informe dispensar OU reabrir, não os dois.");
        }
        if (pedido.valorPago() != null && (dispensar || reabrir)) {
            throw new RegraNegocioException("Para pagar, não envie dispensar nem reabrir.");
        }

        if (reabrir) {
            mes.reabrir();
        }
        if (dispensar) {
            mes.dispensar();
        }
        if (pedido.valorPrevisto() != null) {
            mes.setValorPrevisto(pedido.valorPrevisto());
        }
        if (pedido.valorPago() != null) {
            LocalDate data = pedido.dataPagamento() != null ? pedido.dataPagamento() : hoje();
            mes.registrarPagamento(pedido.valorPago(), data, null);
        }
        if (pedido.observacao() != null) {
            mes.setObservacao(pedido.observacao().isBlank() ? null : pedido.observacao().trim());
        }
        return ContaFixaMesResponse.de(mes, hoje());
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    /**
     * O valor previsto: o esperado (contas fixas de verdade, como o
     * financiamento) ou, nas variáveis, o último valor pago.
     */
    private BigDecimal previsao(ContaFixa conta) {
        if (conta.getValorEsperado() != null) {
            return conta.getValorEsperado();
        }
        return mesRepository.findFirstByContaFixaIdAndValorPagoIsNotNullOrderByCompetenciaDesc(conta.getId())
                .map(ContaFixaMes::getValorPago)
                .orElse(null);
    }

    private ResumoMesResponse resumo(YearMonth competencia) {
        LocalDate hoje = hoje();
        List<ContaFixaMesResponse> contas = mesRepository.findByCompetencia(competencia.atDay(1)).stream()
                .sorted(Comparator.comparing((ContaFixaMes m) -> Objects.toString(m.getContaFixa().getGrupo(), ""))
                        .thenComparing(m -> m.getContaFixa().getNome()))
                .map(mes -> ContaFixaMesResponse.de(mes, hoje))
                .toList();

        BigDecimal totalPrevisto = somar(contas.stream().map(ContaFixaMesResponse::valorEfetivo).toList());
        BigDecimal totalPago = somar(contas.stream().map(ContaFixaMesResponse::valorPago).toList());

        return new ResumoMesResponse(
                competencia,
                totalPrevisto,
                totalPago,
                contar(contas, ContaFixaMes.Situacao.PAGA),
                contar(contas, ContaFixaMes.Situacao.PENDENTE),
                contar(contas, ContaFixaMes.Situacao.ATRASADA),
                contas.stream().filter(c -> c.valorEfetivo() == null).count(),
                contas);
    }

    private BigDecimal somar(List<BigDecimal> valores) {
        return valores.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long contar(List<ContaFixaMesResponse> contas, ContaFixaMes.Situacao situacao) {
        return contas.stream().filter(c -> c.situacao().equals(situacao.name())).count();
    }

    private LocalDate hoje() {
        return LocalDate.now(FUSO_BRASIL);
    }
}
