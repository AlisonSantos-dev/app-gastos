package dev.alisonsantos.appgastos.classificacao;

import dev.alisonsantos.appgastos.transacao.TipoLancamento;
import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reclassifica as transações que já estão no banco.
 *
 * O sync já classifica tudo que entra. Este serviço serve para aplicar
 * as regras nas transações antigas (como as 2.269 da primeira
 * sincronização) e para reaplicar quando as regras melhorarem.
 */
@Service
public class ClassificacaoService {

    private static final Logger log = LoggerFactory.getLogger(ClassificacaoService.class);

    private final TransacaoRepository transacaoRepository;
    private final ClassificadorLancamento classificador;

    public ClassificacaoService(TransacaoRepository transacaoRepository,
                                ClassificadorLancamento classificador) {
        this.transacaoRepository = transacaoRepository;
        this.classificador = classificador;
    }

    @Transactional
    public ResumoClassificacao reclassificarTodas() {
        List<Transacao> transacoes = transacaoRepository.findAll();

        int alteradas = 0;
        int manuais = 0;
        Map<TipoLancamento, Long> porTipo = new EnumMap<>(TipoLancamento.class);

        for (Transacao transacao : transacoes) {
            if (transacao.isClassificacaoManual()) {
                manuais++;
            } else if (classificador.aplicar(transacao, transacao.getConta().getTipo())) {
                alteradas++;
            }
            porTipo.merge(transacao.getTipoLancamento(), 1L, Long::sum);
        }

        log.info("Classificação: {} transações analisadas, {} alteradas, {} manuais preservadas",
                transacoes.size(), alteradas, manuais);
        return new ResumoClassificacao(transacoes.size(), alteradas, manuais, porTipo);
    }
}
