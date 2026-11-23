package dev.alisonsantos.appgastos.divisao;

import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.destino.DestinoPadrao;
import dev.alisonsantos.appgastos.edicao.PropagadorParcelas;
import dev.alisonsantos.appgastos.legenda.AplicadorLegenda;
import dev.alisonsantos.appgastos.legenda.Regra;
import dev.alisonsantos.appgastos.legenda.RegraRepository;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.PessoaService;
import dev.alisonsantos.appgastos.transacao.OrigemPessoa;
import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import dev.alisonsantos.appgastos.transacao.TransacaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Divide uma compra entre várias pessoas (e desfaz a divisão).
 *
 * Numa compra parcelada, a divisão vale para todas as parcelas, nas
 * mesmas proporções; e as parcelas futuras, quando chegarem pelo sync,
 * já vêm divididas (o PropagadorParcelas cuida disso).
 */
@Service
public class DivisaoService {

    private final TransacaoRepository transacaoRepository;
    private final RegraRepository regraRepository;
    private final PessoaService pessoaService;
    private final PropagadorParcelas propagadorParcelas;
    private final AplicadorLegenda aplicadorLegenda;
    private final DestinoPadrao destinoPadrao;
    private final DivisaoSalvaRepository divisaoSalvaRepository;

    public DivisaoService(TransacaoRepository transacaoRepository, RegraRepository regraRepository,
                          PessoaService pessoaService, PropagadorParcelas propagadorParcelas,
                          AplicadorLegenda aplicadorLegenda, DestinoPadrao destinoPadrao,
                          DivisaoSalvaRepository divisaoSalvaRepository) {
        this.transacaoRepository = transacaoRepository;
        this.regraRepository = regraRepository;
        this.pessoaService = pessoaService;
        this.propagadorParcelas = propagadorParcelas;
        this.aplicadorLegenda = aplicadorLegenda;
        this.destinoPadrao = destinoPadrao;
        this.divisaoSalvaRepository = divisaoSalvaRepository;
    }

    @Transactional
    public DivisaoResponse dividir(Long transacaoId, DivisaoRequest pedido) {
        Transacao transacao = encontrar(transacaoId);
        if (!transacao.isContaNosGastos()) {
            throw new RegraNegocioException("Só gastos podem ser divididos. Esta transação é "
                    + transacao.getTipoLancamento() + ".");
        }

        List<CalculadoraDivisao.Entrada> entradas = pedido.partes().stream()
                .map(parte -> new CalculadoraDivisao.Entrada(pessoaAtiva(parte.pessoaId()), parte.valor(), parte.percentual()))
                .toList();

        // A divisão da transação escolhida, no modo pedido
        List<ParteCalculada> partes = CalculadoraDivisao.calcular(pedido.modo(), entradas, transacao.getValor());

        List<Transacao> alvos = pedido.devePropagar()
                ? propagadorParcelas.parcelasDaMesmaCompra(transacao)
                : List.of(transacao);

        for (Transacao alvo : alvos) {
            // As outras parcelas recebem as mesmas proporções, sobre o valor delas
            alvo.aplicarDivisao(alvo == transacao ? partes : CalculadoraDivisao.redistribuir(partes, alvo.getValor()),
                    OrigemPessoa.DIVISAO);
        }
        return resposta(alvos);
    }

    @Transactional
    public DivisaoResponse desfazer(Long transacaoId, boolean propagarParcelas) {
        Transacao transacao = encontrar(transacaoId);
        List<Transacao> alvos = propagarParcelas
                ? propagadorParcelas.parcelasDaMesmaCompra(transacao)
                : List.of(transacao);

        List<Regra> regras = regraRepository.findByAtivaTrue();
        for (Transacao alvo : alvos) {
            if (alvo.isDividida()) {
                alvo.desfazerDivisao();
                // O dono volta a ser decidido pelas regras e pelo cartão, na hora
                aplicadorLegenda.aplicar(alvo, regras);
                destinoPadrao.aplicar(alvo);
            }
        }
        return resposta(alvos);
    }

    /**
     * Divide várias compras de uma vez, com a mesma divisão (um "evento",
     * como o churrasco: as compras do mercado, do açougue e da bebida,
     * em cartões diferentes, todas divididas entre as mesmas pessoas).
     *
     * A divisão vem de uma divisão salva OU do modo + partes do pedido.
     * Só IGUAL e PERCENTUAL: valores fixos não servem para compras diferentes.
     */
    @Transactional
    public DivisaoLoteResponse dividirLote(DivisaoLoteRequest pedido) {
        ModoDivisao modo;
        List<CalculadoraDivisao.Entrada> entradas;

        if (pedido.divisaoSalvaId() != null) {
            DivisaoSalva divisao = divisaoSalvaRepository.findById(pedido.divisaoSalvaId())
                    .orElseThrow(() -> RecursoNaoEncontradoException.de("Divisão salva", pedido.divisaoSalvaId()));
            modo = divisao.getModo();
            entradas = divisao.paraEntradas();
        } else {
            if (pedido.modo() == null || pedido.partes() == null) {
                throw new RegraNegocioException("Informe divisaoSalvaId, ou então o modo e as partes.");
            }
            if (pedido.modo() == ModoDivisao.VALOR) {
                throw new RegraNegocioException(
                        "Para várias compras, use IGUAL ou PERCENTUAL: valores fixos não servem para compras de valores diferentes.");
            }
            modo = pedido.modo();
            entradas = pedido.partes().stream()
                    .map(parte -> new CalculadoraDivisao.Entrada(pessoaAtiva(parte.pessoaId()), null, parte.percentual()))
                    .toList();
        }

        String evento = pedido.evento() == null || pedido.evento().isBlank() ? null : pedido.evento().trim();
        Set<Transacao> divididas = new LinkedHashSet<>();
        List<String> avisos = new ArrayList<>();

        for (Long id : new LinkedHashSet<>(pedido.transacaoIds())) {
            Transacao transacao = transacaoRepository.findById(id).orElse(null);
            if (transacao == null) {
                avisos.add("Transação " + id + ": não encontrada.");
                continue;
            }
            if (!transacao.isContaNosGastos()) {
                avisos.add("Transação " + id + " (" + transacao.getDescricaoExibicao() + "): não é gasto.");
                continue;
            }
            for (Transacao parcela : propagadorParcelas.parcelasDaMesmaCompra(transacao)) {
                parcela.aplicarDivisao(CalculadoraDivisao.calcular(modo, entradas, parcela.getValor()),
                        OrigemPessoa.DIVISAO);
                if (evento != null) {
                    parcela.setObservacao(evento);
                }
                divididas.add(parcela);
            }
        }

        return new DivisaoLoteResponse(divididas.size(), avisos,
                divididas.stream().map(TransacaoResponse::de).toList());
    }

    private DivisaoResponse resposta(List<Transacao> alvos) {
        return new DivisaoResponse(alvos.size(), alvos.stream().map(TransacaoResponse::de).toList());
    }

    private Transacao encontrar(Long id) {
        return transacaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Transação", id));
    }

    private Pessoa pessoaAtiva(Long id) {
        Pessoa pessoa = pessoaService.encontrar(id);
        if (!pessoa.isAtivo()) {
            throw new RegraNegocioException("A pessoa \"" + pessoa.getNome() + "\" está desativada.");
        }
        return pessoa;
    }
}
