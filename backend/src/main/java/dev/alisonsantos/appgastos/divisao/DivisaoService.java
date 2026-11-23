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
import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import dev.alisonsantos.appgastos.transacao.TransacaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public DivisaoService(TransacaoRepository transacaoRepository, RegraRepository regraRepository,
                          PessoaService pessoaService, PropagadorParcelas propagadorParcelas,
                          AplicadorLegenda aplicadorLegenda, DestinoPadrao destinoPadrao) {
        this.transacaoRepository = transacaoRepository;
        this.regraRepository = regraRepository;
        this.pessoaService = pessoaService;
        this.propagadorParcelas = propagadorParcelas;
        this.aplicadorLegenda = aplicadorLegenda;
        this.destinoPadrao = destinoPadrao;
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
            alvo.aplicarDivisao(alvo == transacao ? partes : CalculadoraDivisao.redistribuir(partes, alvo.getValor()));
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
