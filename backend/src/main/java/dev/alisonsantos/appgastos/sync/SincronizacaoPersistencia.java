package dev.alisonsantos.appgastos.sync;

import dev.alisonsantos.appgastos.cartao.Cartao;
import dev.alisonsantos.appgastos.classificacao.ClassificadorLancamento;
import dev.alisonsantos.appgastos.cartao.CartaoRepository;
import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.conta.ContaRepository;
import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.destino.DestinoPadrao;
import dev.alisonsantos.appgastos.edicao.PropagadorParcelas;
import dev.alisonsantos.appgastos.fatura.Fatura;
import dev.alisonsantos.appgastos.fatura.FaturaRepository;
import dev.alisonsantos.appgastos.fatura.StatusFatura;
import dev.alisonsantos.appgastos.legenda.AplicadorLegenda;
import dev.alisonsantos.appgastos.legenda.Regra;
import dev.alisonsantos.appgastos.legenda.RegraRepository;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyAccount;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyBill;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyCreditCardMetadata;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyTransaction;
import dev.alisonsantos.appgastos.transacao.StatusTransacao;
import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Grava no banco o que veio da Pluggy: contas, faturas e transações.
 *
 * Cada método roda numa transação do banco (@Transactional): ou grava
 * tudo daquela conta, ou nada. Se der erro no meio, o banco volta ao
 * estado anterior, sem deixar dados pela metade.
 *
 * REGRA DE OURO: o sync só atualiza o que vem do banco (valor, data,
 * status, cartão, fatura...). Ele NUNCA mexe no que você define:
 * descrição amigável, pessoa (destino), observação e classificação
 * corrigida à mão. A classificação automática é refeita a cada sync.
 */
@Service
public class SincronizacaoPersistencia {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoPersistencia.class);

    private final ContaRepository contaRepository;
    private final CartaoRepository cartaoRepository;
    private final FaturaRepository faturaRepository;
    private final TransacaoRepository transacaoRepository;
    private final ClassificadorLancamento classificador;
    private final DestinoPadrao destinoPadrao;
    private final RegraRepository regraRepository;
    private final AplicadorLegenda aplicadorLegenda;
    private final PropagadorParcelas propagadorParcelas;

    public SincronizacaoPersistencia(ContaRepository contaRepository,
                                     CartaoRepository cartaoRepository,
                                     FaturaRepository faturaRepository,
                                     TransacaoRepository transacaoRepository,
                                     ClassificadorLancamento classificador,
                                     DestinoPadrao destinoPadrao,
                                     RegraRepository regraRepository,
                                     AplicadorLegenda aplicadorLegenda,
                                     PropagadorParcelas propagadorParcelas) {
        this.contaRepository = contaRepository;
        this.cartaoRepository = cartaoRepository;
        this.faturaRepository = faturaRepository;
        this.transacaoRepository = transacaoRepository;
        this.classificador = classificador;
        this.destinoPadrao = destinoPadrao;
        this.regraRepository = regraRepository;
        this.aplicadorLegenda = aplicadorLegenda;
        this.propagadorParcelas = propagadorParcelas;
    }

    // ------------------------------------------------------------------
    // Conta
    // ------------------------------------------------------------------

    /**
     * Cria a conta se for nova. Se já existir, não mexe no nome nem no
     * banco, porque você pode ter renomeado no app.
     *
     * @return o id da conta no nosso banco
     */
    @Transactional
    public Long salvarConta(String itemId, PluggyAccount contaPluggy, ResumoSincronizacao resumo) {
        Conta existente = contaRepository.findByPluggyAccountId(contaPluggy.id()).orElse(null);
        if (existente != null) {
            existente.setPluggyItemId(itemId);
            resumo.contaExistente();
            return existente.getId();
        }

        String nome = primeiroPreenchido(
                PluggyConversor.texto(contaPluggy.marketingName(), 100),
                PluggyConversor.texto(contaPluggy.name(), 100),
                "Conta " + contaPluggy.id());
        String banco = primeiroPreenchido(
                PluggyConversor.texto(contaPluggy.name(), 60),
                "Banco");

        Conta nova = new Conta(banco, nome, PluggyConversor.tipoConta(contaPluggy.type()));
        nova.setPluggyItemId(itemId);
        nova.setPluggyAccountId(contaPluggy.id());
        contaRepository.save(nova);

        resumo.contaNova();
        log.info("Sync: conta nova criada: {} ({})", nome, nova.getTipo());
        return nova.getId();
    }

    // ------------------------------------------------------------------
    // Faturas
    // ------------------------------------------------------------------

    /** Cria ou atualiza as faturas fechadas de uma conta de cartão. */
    @Transactional
    public void salvarFaturas(Long contaId, List<PluggyBill> faturasPluggy, ResumoSincronizacao resumo) {
        Conta conta = contaRepository.getReferenceById(contaId);

        for (PluggyBill faturaPluggy : faturasPluggy) {
            if (faturaPluggy.id() == null || faturaPluggy.dueDate() == null) {
                continue;
            }

            Fatura fatura = faturaRepository.findByPluggyBillId(faturaPluggy.id()).orElse(null);
            if (fatura == null) {
                fatura = new Fatura(conta, PluggyConversor.data(faturaPluggy.dueDate()));
                fatura.setPluggyBillId(faturaPluggy.id());
                resumo.faturaNova();
            } else {
                resumo.faturaAtualizada();
            }

            fatura.setDataVencimento(PluggyConversor.data(faturaPluggy.dueDate()));
            fatura.setDataFechamento(PluggyConversor.data(faturaPluggy.billClosingDate()));
            fatura.setTotalOficial(faturaPluggy.totalAmount());
            fatura.setPagamentoMinimo(faturaPluggy.minimumPaymentAmount());
            fatura.setEncargos(PluggyConversor.encargos(faturaPluggy));

            // Fatura que vem da Pluggy já está fechada. "PAGA" é definido mais à frente.
            if (fatura.getStatus() == StatusFatura.ABERTA) {
                fatura.setStatus(StatusFatura.FECHADA);
            }
            faturaRepository.save(fatura);
        }
    }

    // ------------------------------------------------------------------
    // Transações
    // ------------------------------------------------------------------

    /**
     * Cria as transações novas e atualiza as existentes de uma conta.
     *
     * Para ser rápido, carrega tudo da conta de uma vez (transações,
     * faturas) e trabalha em memória, em vez de consultar o banco a cada
     * transação.
     */
    @Transactional
    public void salvarTransacoes(Long contaId, List<PluggyTransaction> transacoesPluggy,
                                 ResumoSincronizacao resumo) {
        Conta conta = contaRepository.findById(contaId).orElseThrow();
        boolean contaDeCartao = conta.getTipo() == TipoConta.CARTAO_CREDITO;

        // O que já temos no banco, indexado pelo id da Pluggy
        Map<String, Transacao> existentes = transacaoRepository.findByContaId(contaId).stream()
                .collect(Collectors.toMap(Transacao::getPluggyTransactionId, Function.identity()));

        Map<String, Fatura> faturasPorIdPluggy = faturaRepository
                .findByContaIdOrderByDataVencimentoDesc(contaId).stream()
                .filter(fatura -> fatura.getPluggyBillId() != null)
                .collect(Collectors.toMap(Fatura::getPluggyBillId, Function.identity()));

        // As regras da legenda, carregadas uma vez só para a conta inteira
        List<Regra> regras = regraRepository.findByAtivaTrue();

        Map<String, Cartao> cartoesPorFinal = new HashMap<>();
        Set<String> idsRecebidos = new HashSet<>();
        List<Transacao> novas = new ArrayList<>();

        for (PluggyTransaction transacaoPluggy : transacoesPluggy) {
            if (transacaoPluggy.id() == null || transacaoPluggy.date() == null
                    || transacaoPluggy.amount() == null) {
                resumo.transacaoIgnorada();
                continue;
            }
            if (PluggyConversor.isProjecaoEspuria(transacaoPluggy)) {
                resumo.transacaoIgnorada();
                continue;
            }
            idsRecebidos.add(transacaoPluggy.id());

            Transacao transacao = existentes.get(transacaoPluggy.id());
            if (transacao == null) {
                transacao = new Transacao(
                        transacaoPluggy.id(),
                        conta,
                        transacaoPluggy.date(),
                        transacaoPluggy.amount(),
                        PluggyConversor.descricaoBanco(transacaoPluggy),
                        PluggyConversor.tipoMovimento(transacaoPluggy.type()),
                        PluggyConversor.status(transacaoPluggy.status()));
                novas.add(transacao);
                resumo.transacaoNova();
            } else {
                resumo.transacaoExistente();
            }

            atualizarDadosDoBanco(transacao, transacaoPluggy, conta, contaDeCartao,
                    faturasPorIdPluggy, cartoesPorFinal, resumo);

            // Parcela nova de uma compra que você já editou? Ela herda as
            // suas edições (descrição, dono, categoria...), como as irmãs.
            if (transacao.getId() == null) {
                propagadorParcelas.herdarEdicoes(transacao);
            }

            // Classifica (compra, estorno, pagamento de fatura...), menos se
            // você já tiver corrigido a classificação à mão.
            classificador.aplicar(transacao, conta.getTipo());

            // Legenda: nome, categoria e, se a regra disser, o dono.
            aplicadorLegenda.aplicar(transacao, regras);

            // Dono pelo cartão (ou pela conta), menos se você já tiver
            // escolhido à mão ou uma regra tiver decidido.
            destinoPadrao.aplicar(transacao);
        }

        transacaoRepository.saveAll(novas);
        removerPendentesQueSumiram(existentes.values(), idsRecebidos, resumo);
    }

    /**
     * Copia para a transação tudo o que vem do banco.
     * As transações existentes só são gravadas se algum valor mudar:
     * o Hibernate compara antes de mandar o UPDATE.
     */
    private void atualizarDadosDoBanco(Transacao transacao,
                                       PluggyTransaction transacaoPluggy,
                                       Conta conta,
                                       boolean contaDeCartao,
                                       Map<String, Fatura> faturasPorIdPluggy,
                                       Map<String, Cartao> cartoesPorFinal,
                                       ResumoSincronizacao resumo) {
        transacao.setDataTransacao(transacaoPluggy.date());
        transacao.setValor(transacaoPluggy.amount());
        transacao.setStatus(PluggyConversor.status(transacaoPluggy.status()));
        transacao.setTipoMovimento(PluggyConversor.tipoMovimento(transacaoPluggy.type()));
        transacao.setCategoriaPluggy(PluggyConversor.texto(transacaoPluggy.category(), 100));
        transacao.setCategoriaPluggyId(PluggyConversor.texto(transacaoPluggy.categoryId(), 20));
        transacao.setOperacaoPluggy(PluggyConversor.texto(transacaoPluggy.operationType(), 40));
        transacao.setCnpj(transacaoPluggy.merchant() == null ? null
                : PluggyConversor.cnpj(transacaoPluggy.merchant().cnpj()));
        transacao.setMerchantNome(PluggyConversor.nomeEstabelecimento(transacaoPluggy.merchant()));
        transacao.setContraparteNome(PluggyConversor.contraparte(transacaoPluggy));

        PluggyCreditCardMetadata dadosCartao = transacaoPluggy.creditCardMetadata();
        if (dadosCartao == null) {
            return;
        }

        transacao.setDataCompra(dadosCartao.purchaseDate());
        transacao.setMcc(dadosCartao.payeeMCC());
        definirParcela(transacao, dadosCartao);

        // Fatura: pelo billId. A fatura aberta ainda não existe, então fica vazia.
        transacao.setFatura(dadosCartao.billId() == null ? null
                : faturasPorIdPluggy.get(dadosCartao.billId()));

        // Cartão: pelo final. Se for um final novo, cria o cartão como PENDENTE.
        String finalCartao = PluggyConversor.ultimosDigitosCartao(dadosCartao);
        if (contaDeCartao && finalCartao != null) {
            transacao.setCartao(cartoesPorFinal.computeIfAbsent(finalCartao,
                    digitos -> buscarOuCriarCartao(conta, digitos, resumo)));
        }
    }

    /** Só grava a parcela se for coerente (ex.: 2 de 4). O banco exige isso. */
    private void definirParcela(Transacao transacao, PluggyCreditCardMetadata dadosCartao) {
        Integer numero = dadosCartao.installmentNumber();
        Integer total = dadosCartao.totalInstallments();
        if (numero != null && total != null && total >= 1 && numero >= 1 && numero <= total) {
            transacao.setParcela(numero.shortValue(), total.shortValue());
        } else {
            transacao.setParcela(null, null);
        }
    }

    private Cartao buscarOuCriarCartao(Conta conta, String ultimosDigitos, ResumoSincronizacao resumo) {
        return cartaoRepository.findByContaIdAndUltimosDigitos(conta.getId(), ultimosDigitos)
                .orElseGet(() -> {
                    Cartao novo = cartaoRepository.save(new Cartao(conta, ultimosDigitos));
                    resumo.cartaoNovo();
                    log.info("Sync: cartão novo encontrado: final {} na conta {}", ultimosDigitos, conta.getNome());
                    return novo;
                });
    }

    /**
     * Transação PENDENTE que estava no banco e não veio mais da Pluggy:
     * normalmente é porque ela foi confirmada com outro id, ou foi cancelada.
     * Sem remover, ela ficaria duplicada.
     */
    private void removerPendentesQueSumiram(Iterable<Transacao> existentes, Set<String> idsRecebidos,
                                            ResumoSincronizacao resumo) {
        List<Transacao> sumiram = new ArrayList<>();
        for (Transacao transacao : existentes) {
            if (transacao.getStatus() == StatusTransacao.PENDENTE
                    && !idsRecebidos.contains(transacao.getPluggyTransactionId())) {
                sumiram.add(transacao);
            }
        }
        if (!sumiram.isEmpty()) {
            transacaoRepository.deleteAll(sumiram);
            resumo.pendentesRemovidas(sumiram.size());
            log.info("Sync: {} transação(ões) pendente(s) removida(s) por não existirem mais na Pluggy",
                    sumiram.size());
        }
    }

    private String primeiroPreenchido(String... valores) {
        for (String valor : valores) {
            if (valor != null) {
                return valor;
            }
        }
        return null;
    }
}
