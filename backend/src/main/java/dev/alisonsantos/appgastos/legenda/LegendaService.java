package dev.alisonsantos.appgastos.legenda;

import dev.alisonsantos.appgastos.destino.DestinoPadrao;
import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Aplica a legenda em todas as transações e mostra o que falta identificar.
 */
@Service
public class LegendaService {

    private static final Logger log = LoggerFactory.getLogger(LegendaService.class);

    private final TransacaoRepository transacaoRepository;
    private final RegraRepository regraRepository;
    private final AplicadorLegenda aplicador;
    private final DestinoPadrao destinoPadrao;

    public LegendaService(TransacaoRepository transacaoRepository, RegraRepository regraRepository,
                          AplicadorLegenda aplicador, DestinoPadrao destinoPadrao) {
        this.transacaoRepository = transacaoRepository;
        this.regraRepository = regraRepository;
        this.aplicador = aplicador;
        this.destinoPadrao = destinoPadrao;
    }

    /**
     * Reaplica a legenda e, logo depois, o destino padrão.
     * A ordem importa: se uma regra deixar de definir o dono, o destino
     * padrão (pelo cartão) assume no mesmo passo.
     */
    @Transactional
    public ResumoLegenda reaplicarTodas() {
        List<Regra> regras = regraRepository.findByAtivaTrue();
        List<Transacao> transacoes = transacaoRepository.findAll();

        int alteradas = 0;
        long gastos = 0;
        long identificados = 0;

        for (Transacao transacao : transacoes) {
            boolean mudouLegenda = aplicador.aplicar(transacao, regras);
            boolean mudouDestino = destinoPadrao.aplicar(transacao);
            if (mudouLegenda || mudouDestino) {
                alteradas++;
            }
            if (transacao.isContaNosGastos()) {
                gastos++;
                if (transacao.getRegra() != null) {
                    identificados++;
                }
            }
        }

        double cobertura = gastos == 0 ? 0 : BigDecimal.valueOf(identificados * 100.0 / gastos)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();

        log.info("Legenda: {} regras ativas, {} transações, {} alteradas, cobertura de {}%",
                regras.size(), transacoes.size(), alteradas, cobertura);
        return new ResumoLegenda(transacoes.size(), alteradas, gastos, identificados, cobertura);
    }

    /** As descrições sem regra mais frequentes: a lista do que vale a pena identificar. */
    @Transactional(readOnly = true)
    public List<RegraRepository.DescricaoPendente> pendentes(int limite) {
        int limiteSeguro = Math.min(Math.max(limite, 1), 200);
        return regraRepository.buscarDescricoesSemRegra(PageRequest.of(0, limiteSeguro));
    }
}
