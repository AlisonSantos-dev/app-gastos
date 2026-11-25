package dev.alisonsantos.appgastos.fechamento;

import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.conta.ContaRepository;
import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.contafixa.ContaFixaMes;
import dev.alisonsantos.appgastos.contafixa.ContaFixaMesRepository;
import dev.alisonsantos.appgastos.contafixa.StatusContaMes;
import dev.alisonsantos.appgastos.fatura.Fatura;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * O fechamento do mês.
 *
 * PRÉVIA: calculada na hora, com tudo o que existe agora. Muda sempre que
 * uma compra, regra, divisão ou conta muda.
 *
 * PUBLICADO: a foto congelada. Depois de publicado, o mês não muda mais
 * (os membros veem exatamente o que foi publicado). Para corrigir, é
 * preciso reabrir e publicar de novo.
 */
@Service
public class FechamentoService {

    private static final Logger log = LoggerFactory.getLogger(FechamentoService.class);
    private static final ZoneId FUSO_BRASIL = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal TOLERANCIA = new BigDecimal("1.00");

    private final FechamentoRepository repository;
    private final ContaFixaMesRepository contaFixaMesRepository;
    private final ContaRepository contaRepository;
    private final CalculadoraFechamento calculadora;

    public FechamentoService(FechamentoRepository repository, ContaFixaMesRepository contaFixaMesRepository,
                             ContaRepository contaRepository, CalculadoraFechamento calculadora) {
        this.repository = repository;
        this.contaFixaMesRepository = contaFixaMesRepository;
        this.contaRepository = contaRepository;
        this.calculadora = calculadora;
    }

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    /** O mês publicado, se existir; senão, a prévia. */
    @Transactional(readOnly = true)
    public FechamentoResponse consultar(YearMonth competencia) {
        return repository.findByCompetencia(competencia.atDay(1))
                .map(this::respostaPublicado)
                .orElseGet(() -> respostaPrevia(competencia, calcular(competencia)));
    }

    /** O detalhe: as linhas de uma pessoa (ou as sem dono) no mês. */
    @Transactional(readOnly = true)
    public List<ItemFechamentoResponse> itens(YearMonth competencia, Long pessoaId, boolean semDono) {
        List<LinhaFechamento> linhas = repository.findByCompetencia(competencia.atDay(1))
                .map(Fechamento::linhas)
                .orElseGet(() -> calcular(competencia).linhas());

        return linhas.stream()
                .filter(l -> semDono ? l.pessoa() == null
                        : pessoaId == null || (l.pessoa() != null && pessoaId.equals(l.pessoa().getId())))
                .sorted(Comparator.comparing(LinhaFechamento::origem).thenComparing(LinhaFechamento::data))
                .map(ItemFechamentoResponse::de)
                .toList();
    }

    // ------------------------------------------------------------------
    // Publicação
    // ------------------------------------------------------------------

    /**
     * Publica o mês (congela a foto). Se houver alertas bloqueantes, só
     * publica com forcar = true.
     */
    @Transactional
    public FechamentoResponse publicar(YearMonth competencia, boolean forcar) {
        if (repository.existsByCompetencia(competencia.atDay(1))) {
            throw new RegraNegocioException("O mês " + competencia + " já foi publicado. Para mudar, reabra primeiro.");
        }

        Calculo calculo = calcular(competencia);
        List<Alerta> bloqueantes = calculo.alertas().stream().filter(Alerta::bloqueante).toList();
        if (!bloqueantes.isEmpty() && !forcar) {
            throw new RegraNegocioException("O mês tem pendências: "
                    + bloqueantes.stream().map(Alerta::mensagem).collect(Collectors.joining(" | "))
                    + ". Resolva ou publique com forcar=true.");
        }
        if (calculo.linhas().isEmpty()) {
            throw new RegraNegocioException("O mês " + competencia + " não tem nenhum gasto para publicar.");
        }

        Fechamento fechamento = repository.save(new Fechamento(competencia, calculo.linhas()));
        log.info("Fechamento {} publicado: {} linhas, total R$ {}", competencia,
                calculo.linhas().size(), fechamento.getTotal());
        return respostaPublicado(fechamento);
    }

    /** Reabre o mês: apaga a foto. Os recebimentos registrados continuam. */
    @Transactional
    public void reabrir(YearMonth competencia) {
        Fechamento fechamento = repository.findByCompetencia(competencia.atDay(1))
                .orElseThrow(() -> new RegraNegocioException("O mês " + competencia + " não está publicado."));
        repository.delete(fechamento);
    }

    // ------------------------------------------------------------------
    // O cálculo (prévia)
    // ------------------------------------------------------------------

    private record Calculo(List<LinhaFechamento> linhas, List<ConferenciaFatura> conferencia, List<Alerta> alertas) {
    }

    private Calculo calcular(YearMonth competencia) {
        var inicioData = competencia.atDay(1);
        var fimData = competencia.plusMonths(1).atDay(1);
        Instant inicio = inicioData.atStartOfDay(FUSO_BRASIL).toInstant();
        Instant fim = fimData.atStartOfDay(FUSO_BRASIL).toInstant();

        List<Transacao> compras = repository.comprasDasFaturas(inicioData, fimData);
        List<Transacao> contaCorrente = repository.gastosDaContaCorrente(inicio, fim);
        List<ContaFixaMes> contasFixas = contaFixaMesRepository.findByCompetencia(inicioData);
        List<Fatura> faturas = repository.faturasDoMes(inicioData, fimData);

        List<LinhaFechamento> linhas = calculadora.montar(compras, contaCorrente, contasFixas);
        List<ConferenciaFatura> conferencia = conferir(faturas, compras);
        List<Alerta> alertas = alertas(competencia, linhas, contasFixas, conferencia, faturas);
        return new Calculo(linhas, conferencia, alertas);
    }

    private List<ConferenciaFatura> conferir(List<Fatura> faturas, List<Transacao> compras) {
        return faturas.stream()
                .map(f -> {
                    BigDecimal soma = compras.stream()
                            .filter(t -> t.getFatura().getId().equals(f.getId()))
                            .map(Transacao::getValor)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal diferenca = f.getTotalOficial() == null ? null : f.getTotalOficial().subtract(soma);
                    boolean bate = diferenca != null && diferenca.abs().compareTo(TOLERANCIA) <= 0;
                    return new ConferenciaFatura(f.getId(), f.getConta().getNome(), f.getDataVencimento(),
                            f.getTotalOficial(), soma, diferenca, bate);
                })
                .sorted(Comparator.comparing(ConferenciaFatura::vencimento))
                .toList();
    }

    private List<Alerta> alertas(YearMonth competencia, List<LinhaFechamento> linhas, List<ContaFixaMes> contasFixas,
                                 List<ConferenciaFatura> conferencia, List<Fatura> faturas) {
        List<Alerta> alertas = new ArrayList<>();

        TotalPessoa semDono = calculadora.totalSemDono(linhas);
        if (semDono != null) {
            alertas.add(new Alerta(Alerta.Tipo.SEM_DONO, semDono.itens() + " lançamento(s) sem dono, somando R$ "
                    + semDono.total() + ": resolva na caixa de entrada", true));
        }

        long semValor = contasFixas.stream()
                .filter(c -> c.getStatus() != StatusContaMes.DISPENSADA && c.valorEfetivo() == null)
                .count();
        if (semValor > 0) {
            alertas.add(new Alerta(Alerta.Tipo.CONTA_SEM_VALOR, semValor
                    + " conta(s) fixa(s) sem valor: informe a previsão ou o pagamento", true));
        }

        long pendentes = contasFixas.stream()
                .filter(c -> c.getStatus() == StatusContaMes.PENDENTE && c.valorEfetivo() != null)
                .count();
        if (pendentes > 0) {
            alertas.add(new Alerta(Alerta.Tipo.CONTA_PENDENTE, pendentes
                    + " conta(s) fixa(s) ainda não paga(s): entram com o valor previsto", false));
        }

        conferencia.stream().filter(c -> !c.bate()).forEach(c -> alertas.add(new Alerta(Alerta.Tipo.FATURA_NAO_BATE,
                "A fatura " + c.cartao() + " de " + c.vencimento() + " não bate: diferença de R$ " + c.diferenca(), true)));

        Set<Long> contasComFatura = faturas.stream().map(f -> f.getConta().getId()).collect(Collectors.toSet());
        contaRepository.findAll().stream()
                .filter(c -> c.getTipo() == TipoConta.CARTAO_CREDITO && c.isAtiva())
                .filter(c -> !contasComFatura.contains(c.getId()))
                .map(Conta::getNome)
                .filter(Objects::nonNull)
                .forEach(nome -> alertas.add(new Alerta(Alerta.Tipo.FATURA_AUSENTE,
                        "O cartão " + nome + " ainda não tem a fatura que vence em " + competencia
                                + " (ela fecha depois, ou o cartão não é mais usado)", true)));

        return alertas;
    }

    // ------------------------------------------------------------------
    // As respostas
    // ------------------------------------------------------------------

    private FechamentoResponse respostaPrevia(YearMonth competencia, Calculo calculo) {
        return new FechamentoResponse(competencia, "PREVIA", null,
                total(calculo.linhas()),
                calculadora.totaisPorPessoa(calculo.linhas()),
                calculadora.totalSemDono(calculo.linhas()),
                calculo.conferencia(),
                calculo.alertas());
    }

    private FechamentoResponse respostaPublicado(Fechamento fechamento) {
        List<LinhaFechamento> linhas = fechamento.linhas();
        return new FechamentoResponse(fechamento.getCompetencia(), "PUBLICADO", fechamento.getPublicadoEm(),
                fechamento.getTotal(),
                calculadora.totaisPorPessoa(linhas),
                calculadora.totalSemDono(linhas),
                List.of(),
                List.of());
    }

    private BigDecimal total(List<LinhaFechamento> linhas) {
        return linhas.stream().map(LinhaFechamento::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
