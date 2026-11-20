package dev.alisonsantos.appgastos.sync;

import dev.alisonsantos.appgastos.pluggy.PluggyApiClient;
import dev.alisonsantos.appgastos.pluggy.PluggyProperties;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orquestra a sincronização com a Pluggy.
 *
 * Para cada banco configurado no .env:
 *   1. busca as contas e grava as novas;
 *   2. se for cartão, busca e grava as faturas;
 *   3. busca todas as transações e grava/atualiza.
 *
 * As chamadas à Pluggy acontecem AQUI, fora de transação do banco.
 * A gravação fica na SincronizacaoPersistencia, com @Transactional.
 * Assim o banco de dados não fica "preso" esperando a resposta da internet.
 *
 * Um erro numa conta não derruba as outras: ele é anotado no resumo,
 * e a sincronização segue para a próxima.
 */
@Service
public class SincronizacaoService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoService.class);

    private final PluggyApiClient apiClient;
    private final PluggyProperties properties;
    private final SincronizacaoPersistencia persistencia;

    /** Impede duas sincronizações ao mesmo tempo (ex.: clique duplo). */
    private final AtomicBoolean emAndamento = new AtomicBoolean(false);

    public SincronizacaoService(PluggyApiClient apiClient,
                                PluggyProperties properties,
                                SincronizacaoPersistencia persistencia) {
        this.apiClient = apiClient;
        this.properties = properties;
        this.persistencia = persistencia;
    }

    /**
     * Sincroniza todos os bancos.
     *
     * @throws IllegalStateException se já houver uma sincronização rodando
     */
    public ResumoSincronizacao sincronizar() {
        if (!emAndamento.compareAndSet(false, true)) {
            throw new IllegalStateException("Já existe uma sincronização em andamento. Aguarde ela terminar.");
        }

        ResumoSincronizacao resumo = new ResumoSincronizacao();
        try {
            log.info("Sync: iniciando sincronização de {} banco(s)", properties.itemIds().size());
            if (properties.itemIds().isEmpty()) {
                resumo.adicionarErro("Nenhum banco configurado. Defina PLUGGY_ITEM_IDS no .env.");
            }
            for (String itemId : properties.itemIds()) {
                sincronizarBanco(itemId.trim(), resumo);
            }
        } finally {
            resumo.finalizar();
            emAndamento.set(false);
        }

        log.info("Sync: concluída em {}s: {}", resumo.getDuracaoSegundos(), resumo);
        return resumo;
    }

    private void sincronizarBanco(String itemId, ResumoSincronizacao resumo) {
        List<PluggyAccount> contas;
        try {
            contas = apiClient.listarContas(itemId);
        } catch (RuntimeException e) {
            log.error("Sync: erro ao buscar as contas do item {}", itemId, e);
            resumo.adicionarErro("Banco " + itemId + ": " + e.getMessage());
            return;
        }

        for (PluggyAccount conta : contas) {
            try {
                Long contaId = persistencia.salvarConta(itemId, conta, resumo);

                if ("CREDIT".equals(conta.type())) {
                    persistencia.salvarFaturas(contaId, apiClient.listarFaturas(conta.id()), resumo);
                }

                // Sem datas: busca todo o histórico disponível na Pluggy
                persistencia.salvarTransacoes(contaId, apiClient.listarTransacoes(conta.id(), null, null), resumo);

            } catch (RuntimeException e) {
                log.error("Sync: erro ao sincronizar a conta {}", conta.id(), e);
                resumo.adicionarErro("Conta " + PluggyConversor.texto(conta.name(), 60) + ": " + e.getMessage());
            }
        }
    }
}
