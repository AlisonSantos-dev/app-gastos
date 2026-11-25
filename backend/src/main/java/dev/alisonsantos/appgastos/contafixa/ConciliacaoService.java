package dev.alisonsantos.appgastos.contafixa;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/**
 * Liga as contas fixas aos pagamentos do extrato.
 *
 * Quando o pagamento é encontrado:
 *   - a conta do mês vira PAGA, com o valor e a data reais;
 *   - a transação do extrato é marcada como revisada (sai da caixa de
 *     entrada: ela já está explicada, é a Sanepar).
 *
 * No fechamento (Etapa 17), essa transação não é somada de novo: quem
 * entra no total é a conta do mês, com o rateio dela.
 */
@Service
public class ConciliacaoService {

    private static final Logger log = LoggerFactory.getLogger(ConciliacaoService.class);
    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");

    private final ContaFixaMesRepository mesRepository;
    private final ContaFixaMesService mesService;
    private final TransacaoRepository transacaoRepository;
    private final ConciliadorPagamentos conciliador;

    public ConciliacaoService(ContaFixaMesRepository mesRepository, ContaFixaMesService mesService,
                              TransacaoRepository transacaoRepository, ConciliadorPagamentos conciliador) {
        this.mesRepository = mesRepository;
        this.mesService = mesService;
        this.transacaoRepository = transacaoRepository;
        this.conciliador = conciliador;
    }

    /** Gera o mês (se faltar alguma conta) e procura os pagamentos das pendentes. */
    @Transactional
    public ResultadoConciliacao conciliar(YearMonth competencia) {
        mesService.gerar(competencia);

        List<ContaFixaMes> contasDoMes = mesRepository.findByCompetencia(competencia.atDay(1));

        // Os candidatos: do mês anterior ao seguinte (a janela de 20 dias pode passar da virada do mês)
        Instant inicio = competencia.minusMonths(1).atDay(1).atStartOfDay(FUSO_BRASIL).toInstant();
        Instant fim = competencia.plusMonths(2).atDay(1).atStartOfDay(FUSO_BRASIL).toInstant();
        List<Transacao> candidatos = mesRepository.buscarPagamentosCandidatos(inicio, fim);

        List<VinculoResponse> encontrados = conciliador.conciliar(contasDoMes, candidatos).stream()
                .map(vinculo -> vincular(vinculo.conta(), vinculo.pagamento()))
                .toList();

        log.info("Contas fixas {}: {} pagamento(s) encontrado(s)", competencia, encontrados.size());
        return new ResultadoConciliacao(competencia, encontrados, mesService.listar(competencia));
    }

    /** Escolhe à mão o pagamento de uma conta do mês (quando o automático não achou ou achou errado). */
    @Transactional
    public ContaFixaMesResponse vincularManualmente(Long contaDoMesId, Long transacaoId) {
        ContaFixaMes mes = mesRepository.findById(contaDoMesId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Conta do mês", contaDoMesId));
        Transacao transacao = transacaoRepository.findById(transacaoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Transação", transacaoId));

        if (transacao.getValor().signum() >= 0) {
            throw new RegraNegocioException("O pagamento precisa ser uma saída (valor negativo).");
        }
        boolean jaEhDestaConta = mes.getTransacao() != null && mes.getTransacao().getId().equals(transacaoId);
        if (!jaEhDestaConta && mesRepository.existsByTransacaoId(transacaoId)) {
            throw new RegraNegocioException("Essa transação já é o pagamento de outra conta fixa.");
        }

        vincular(mes, transacao);
        return ContaFixaMesResponse.de(mes, LocalDate.now(FUSO_BRASIL));
    }

    /**
     * Chamado no fim de cada sincronização: procura os pagamentos do mês
     * atual e do anterior (o que foi pago na virada do mês).
     */
    @Transactional
    public int conciliarAposSync() {
        YearMonth atual = YearMonth.now(FUSO_BRASIL);
        return conciliar(atual.minusMonths(1)).encontrados().size()
                + conciliar(atual).encontrados().size();
    }

    private VinculoResponse vincular(ContaFixaMes mes, Transacao pagamento) {
        LocalDate data = ConciliadorPagamentos.data(pagamento);
        mes.registrarPagamento(pagamento.getValor().abs(), data, pagamento);
        pagamento.marcarRevisada();

        return new VinculoResponse(mes.getId(), mes.getContaFixa().getNome(), pagamento.getId(),
                pagamento.getDescricaoBanco(), pagamento.getValor().abs(), data);
    }
}
