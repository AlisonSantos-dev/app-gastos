package dev.alisonsantos.appgastos.destino;

import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reaplica o destino padrão em lotes de transações:
 * todas, as de um cartão ou as sem cartão de uma conta.
 */
@Service
public class DestinoService {

    private static final Logger log = LoggerFactory.getLogger(DestinoService.class);

    private final TransacaoRepository transacaoRepository;
    private final DestinoPadrao destinoPadrao;

    public DestinoService(TransacaoRepository transacaoRepository, DestinoPadrao destinoPadrao) {
        this.transacaoRepository = transacaoRepository;
        this.destinoPadrao = destinoPadrao;
    }

    @Transactional
    public ResumoDestino reaplicarTodas() {
        return reaplicar(transacaoRepository.findAll(), "todas");
    }

    @Transactional
    public ResumoDestino reaplicarCartao(Long cartaoId) {
        return reaplicar(transacaoRepository.findByCartaoId(cartaoId), "cartão " + cartaoId);
    }

    @Transactional
    public ResumoDestino reaplicarConta(Long contaId) {
        return reaplicar(transacaoRepository.findByContaIdAndCartaoIsNull(contaId), "conta " + contaId);
    }

    private ResumoDestino reaplicar(List<Transacao> transacoes, String escopo) {
        int alteradas = 0;
        long semDono = 0;
        Map<String, Long> porPessoa = new TreeMap<>();

        for (Transacao transacao : transacoes) {
            if (destinoPadrao.aplicar(transacao)) {
                alteradas++;
            }
            if (transacao.isContaNosGastos()) {
                if (transacao.getPessoa() == null) {
                    semDono++;
                } else {
                    porPessoa.merge(transacao.getPessoa().getNome(), 1L, Long::sum);
                }
            }
        }

        log.info("Destino ({}): {} transações, {} alteradas, {} gastos sem dono",
                escopo, transacoes.size(), alteradas, semDono);
        return new ResumoDestino(transacoes.size(), alteradas, porPessoa, semDono);
    }
}
