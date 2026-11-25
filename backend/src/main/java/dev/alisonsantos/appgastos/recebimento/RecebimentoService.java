package dev.alisonsantos.appgastos.recebimento;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.fechamento.FechamentoRepository;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.PessoaRepository;
import dev.alisonsantos.appgastos.pessoa.PessoaService;
import dev.alisonsantos.appgastos.transacao.TipoLancamento;
import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Os recebimentos: quem já pagou o que deve ao titular.
 *
 * O "devido" de cada pessoa vem do fechamento PUBLICADO (a foto do mês).
 * Os Pix recebidos viram sugestões pelo nome no extrato, e só viram
 * recebimento quando você confirma (o "tique").
 */
@Service
public class RecebimentoService {

    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");

    private final RecebimentoRepository repository;
    private final FechamentoRepository fechamentoRepository;
    private final PessoaRepository pessoaRepository;
    private final PessoaService pessoaService;
    private final TransacaoRepository transacaoRepository;

    public RecebimentoService(RecebimentoRepository repository, FechamentoRepository fechamentoRepository,
                              PessoaRepository pessoaRepository, PessoaService pessoaService,
                              TransacaoRepository transacaoRepository) {
        this.repository = repository;
        this.fechamentoRepository = fechamentoRepository;
        this.pessoaRepository = pessoaRepository;
        this.pessoaService = pessoaService;
        this.transacaoRepository = transacaoRepository;
    }

    // ------------------------------------------------------------------
    // Painel do mês
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PainelRecebimentosResponse painel(YearMonth competencia) {
        boolean publicado = fechamentoRepository.existsByCompetencia(competencia.atDay(1));

        Map<Long, BigDecimal> devidos = new HashMap<>();
        fechamentoRepository.devidosPorPessoaEMes().stream()
                .filter(d -> d.getCompetencia().equals(competencia.atDay(1)))
                .forEach(d -> devidos.put(d.getPessoaId(), d.getTotal()));

        Map<Long, List<Recebimento>> recebimentosPorPessoa = new HashMap<>();
        repository.findByCompetenciaOrderByDataAsc(competencia.atDay(1))
                .forEach(r -> recebimentosPorPessoa.computeIfAbsent(r.getPessoa().getId(), k -> new ArrayList<>()).add(r));

        List<SaldoPessoaResponse> pessoas = new ArrayList<>();
        for (Pessoa pessoa : pessoaRepository.findByAtivoTrueOrderByNomeAsc()) {
            if (pessoa.isTitular()) {
                continue; // o titular não deve a si mesmo
            }
            BigDecimal devido = publicado ? devidos.getOrDefault(pessoa.getId(), BigDecimal.ZERO) : null;
            List<Recebimento> recebimentos = recebimentosPorPessoa.getOrDefault(pessoa.getId(), List.of());
            BigDecimal recebido = somar(recebimentos);

            boolean temMovimento = (devido != null && devido.signum() != 0) || !recebimentos.isEmpty();
            if (!temMovimento) {
                continue;
            }
            pessoas.add(new SaldoPessoaResponse(
                    pessoa.getId(), pessoa.getNome(), pessoa.getCor(),
                    devido, recebido,
                    devido == null ? null : devido.subtract(recebido),
                    SituacaoRecebimento.de(devido, recebido).name(),
                    recebimentos.stream().map(RecebimentoResponse::de).toList()));
        }
        pessoas.sort(Comparator.comparing((SaldoPessoaResponse s) -> s.saldo() == null ? BigDecimal.ZERO : s.saldo())
                .reversed());

        BigDecimal totalDevido = pessoas.stream().map(SaldoPessoaResponse::devido)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRecebido = pessoas.stream().map(SaldoPessoaResponse::recebido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal aReceber = pessoas.stream().map(SaldoPessoaResponse::saldo)
                .filter(v -> v != null && v.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PainelRecebimentosResponse(competencia, publicado, totalDevido, totalRecebido, aReceber, pessoas);
    }

    // ------------------------------------------------------------------
    // Sugestões (os Pix recebidos)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SugestaoRecebimentoResponse> sugestoes(int dias) {
        int diasSeguros = Math.min(Math.max(dias, 1), 365);
        Instant desde = LocalDate.now(FUSO_BRASIL).minusDays(diasSeguros).atStartOfDay(FUSO_BRASIL).toInstant();

        List<Pessoa> comNome = pessoaRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(p -> !p.isTitular() && p.getNomeNoExtrato() != null)
                .toList();
        Map<Long, TreeMap<YearMonth, BigDecimal>> saldos = saldosEmAberto();

        List<SugestaoRecebimentoResponse> sugestoes = new ArrayList<>();
        for (Transacao entrada : repository.entradasSemRecebimento(desde)) {
            Optional<Pessoa> pessoa = comNome.stream()
                    .filter(p -> NomeNoExtrato.bate(entrada.getContraparteNome(), p.getNomeNoExtrato()))
                    .findFirst();
            pessoa.ifPresent(p -> {
                LocalDate data = LocalDate.ofInstant(entrada.getDataTransacao(), FUSO_BRASIL);
                sugestoes.add(new SugestaoRecebimentoResponse(
                        entrada.getId(), data, entrada.getValor(), entrada.getContraparteNome(),
                        entrada.getConta().getNome(), p.getId(), p.getNome(),
                        competenciaSugerida(saldos.get(p.getId()), data)));
            });
        }
        return sugestoes;
    }

    // ------------------------------------------------------------------
    // Registrar e desfazer
    // ------------------------------------------------------------------

    @Transactional
    public RecebimentoResponse registrar(RecebimentoRequest pedido) {
        Pessoa pessoa = pessoaService.encontrar(pedido.pessoaId());
        if (!pessoa.isAtivo()) {
            throw new RegraNegocioException("A pessoa \"" + pessoa.getNome() + "\" está desativada.");
        }

        Transacao transacao = null;
        BigDecimal valor = pedido.valor();
        LocalDate data = pedido.data();

        if (pedido.transacaoId() != null) {
            transacao = transacaoRepository.findById(pedido.transacaoId())
                    .orElseThrow(() -> RecursoNaoEncontradoException.de("Transação", pedido.transacaoId()));
            if (transacao.getTipoLancamento() != TipoLancamento.RECEBIMENTO || transacao.getValor().signum() <= 0) {
                throw new RegraNegocioException("A transação " + transacao.getId() + " não é uma entrada de dinheiro.");
            }
            if (repository.existsByTransacaoId(transacao.getId())) {
                throw new RegraNegocioException("Esse Pix já foi registrado como recebimento.");
            }
            valor = valor != null ? valor : transacao.getValor();
            data = data != null ? data : LocalDate.ofInstant(transacao.getDataTransacao(), FUSO_BRASIL);
            transacao.marcarRevisada();
        }

        if (valor == null) {
            throw new RegraNegocioException("Informe o valor (ou o transacaoId do Pix recebido).");
        }

        String observacao = pedido.observacao() == null || pedido.observacao().isBlank() ? null : pedido.observacao().trim();
        Recebimento recebimento = repository.save(new Recebimento(pessoa, pedido.competencia(), valor,
                data != null ? data : LocalDate.now(FUSO_BRASIL), transacao, observacao));
        return RecebimentoResponse.de(recebimento);
    }

    @Transactional
    public void remover(Long id) {
        Recebimento recebimento = repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Recebimento", id));
        repository.delete(recebimento);
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    /** Por pessoa, os meses publicados em que ela ainda deve (mês → saldo), em ordem. */
    private Map<Long, TreeMap<YearMonth, BigDecimal>> saldosEmAberto() {
        Map<Long, TreeMap<YearMonth, BigDecimal>> saldos = new HashMap<>();
        fechamentoRepository.devidosPorPessoaEMes().forEach(d -> saldos
                .computeIfAbsent(d.getPessoaId(), k -> new TreeMap<>())
                .merge(YearMonth.from(d.getCompetencia()), d.getTotal(), BigDecimal::add));
        repository.recebidosPorPessoaEMes().forEach(r -> saldos
                .computeIfAbsent(r.getPessoaId(), k -> new TreeMap<>())
                .merge(YearMonth.from(r.getCompetencia()), r.getTotal().negate(), BigDecimal::add));
        saldos.values().forEach(meses -> meses.values().removeIf(saldo -> saldo.compareTo(SituacaoRecebimento.TOLERANCIA) <= 0));
        return saldos;
    }

    /** O mês mais antigo que ela ainda deve; se não deve nada, o mês anterior ao Pix. */
    private YearMonth competenciaSugerida(TreeMap<YearMonth, BigDecimal> mesesEmAberto, LocalDate dataDoPix) {
        if (mesesEmAberto != null && !mesesEmAberto.isEmpty()) {
            return mesesEmAberto.firstKey();
        }
        return YearMonth.from(dataDoPix).minusMonths(1);
    }

    private BigDecimal somar(List<Recebimento> recebimentos) {
        return recebimentos.stream().map(Recebimento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
