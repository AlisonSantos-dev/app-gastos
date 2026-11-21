package dev.alisonsantos.appgastos.edicao;

import dev.alisonsantos.appgastos.classificacao.ClassificadorLancamento;
import dev.alisonsantos.appgastos.comum.RecursoNaoEncontradoException;
import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.destino.DestinoPadrao;
import dev.alisonsantos.appgastos.legenda.AplicadorLegenda;
import dev.alisonsantos.appgastos.legenda.Regra;
import dev.alisonsantos.appgastos.legenda.RegraRepository;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.PessoaService;
import dev.alisonsantos.appgastos.transacao.OrigemClassificacao;
import dev.alisonsantos.appgastos.transacao.OrigemDescricao;
import dev.alisonsantos.appgastos.transacao.OrigemPessoa;
import dev.alisonsantos.appgastos.transacao.TipoLancamento;
import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import dev.alisonsantos.appgastos.transacao.TransacaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Edição manual de transações.
 *
 * O que você define à mão vira MANUAL e nunca mais é sobrescrito pelo
 * sync nem pelas regras. O que você RESTAURA volta a ser decidido pelas
 * regras automáticas, na hora.
 *
 * Se a compra for parcelada, a edição vale para todas as parcelas
 * (a menos que o pedido diga "propagarParcelas": false).
 */
@Service
public class EdicaoTransacaoService {

    /** Os tipos que, por padrão, não entram nos gastos. */
    private static final Set<TipoLancamento> FORA_DOS_GASTOS = EnumSet.of(
            TipoLancamento.PAGAMENTO_FATURA,
            TipoLancamento.TRANSFERENCIA_PROPRIA,
            TipoLancamento.RECEBIMENTO);

    private final TransacaoRepository transacaoRepository;
    private final RegraRepository regraRepository;
    private final PessoaService pessoaService;
    private final PropagadorParcelas propagadorParcelas;
    private final ClassificadorLancamento classificador;
    private final AplicadorLegenda aplicadorLegenda;
    private final DestinoPadrao destinoPadrao;

    public EdicaoTransacaoService(TransacaoRepository transacaoRepository,
                                  RegraRepository regraRepository,
                                  PessoaService pessoaService,
                                  PropagadorParcelas propagadorParcelas,
                                  ClassificadorLancamento classificador,
                                  AplicadorLegenda aplicadorLegenda,
                                  DestinoPadrao destinoPadrao) {
        this.transacaoRepository = transacaoRepository;
        this.regraRepository = regraRepository;
        this.pessoaService = pessoaService;
        this.propagadorParcelas = propagadorParcelas;
        this.classificador = classificador;
        this.aplicadorLegenda = aplicadorLegenda;
        this.destinoPadrao = destinoPadrao;
    }

    @Transactional
    public EdicaoTransacaoResponse editar(Long id, EdicaoTransacaoRequest pedido) {
        validarPedido(pedido);

        Transacao transacao = transacaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Transação", id));

        Pessoa pessoa = pedido.pessoaId() == null ? null : pessoaAtiva(pedido.pessoaId());

        List<Transacao> alvos = pedido.devePropagar()
                ? propagadorParcelas.parcelasDaMesmaCompra(transacao)
                : List.of(transacao);

        List<Regra> regras = regraRepository.findByAtivaTrue();
        for (Transacao alvo : alvos) {
            aplicarEdicao(alvo, pedido, pessoa);
            reaplicarAutomaticos(alvo, regras);
        }

        return new EdicaoTransacaoResponse(alvos.size(),
                alvos.stream().map(TransacaoResponse::de).toList());
    }

    // ------------------------------------------------------------------
    // A edição de uma transação
    // ------------------------------------------------------------------

    private void aplicarEdicao(Transacao t, EdicaoTransacaoRequest pedido, Pessoa pessoa) {
        // Classificação primeiro: ela decide se a transação é gasto (e se pode ter dono)
        if (pedido.querRestaurarClassificacao()) {
            t.definirClassificacao(t.getTipoLancamento(), t.isContaNosGastos(), OrigemClassificacao.AUTOMATICA);
        } else if (pedido.tipoLancamento() != null || pedido.contaNosGastos() != null) {
            TipoLancamento tipo = pedido.tipoLancamento() != null ? pedido.tipoLancamento() : t.getTipoLancamento();
            boolean contaNosGastos = pedido.contaNosGastos() != null
                    ? pedido.contaNosGastos()
                    : !FORA_DOS_GASTOS.contains(tipo);
            t.definirClassificacao(tipo, contaNosGastos, OrigemClassificacao.MANUAL);

            // Deixou de ser gasto? Então não tem mais dono, nem o escolhido à mão
            if (!contaNosGastos && t.getOrigemPessoa() == OrigemPessoa.MANUAL) {
                t.definirPessoa(null, null);
            }
        }

        // Descrição
        if (pedido.querRestaurarDescricao()) {
            t.definirDescricao(null, OrigemDescricao.BANCO);
        } else if (pedido.descricao() != null) {
            t.definirDescricao(pedido.descricao().trim(), OrigemDescricao.MANUAL);
        }

        // Categoria
        if (pedido.querRestaurarCategoria()) {
            t.setCategoriaManual(false);
        } else if (pedido.categoria() != null) {
            t.setCategoria(pedido.categoria().trim());
            t.setCategoriaManual(true);
        }

        // Pessoa
        if (pedido.querRestaurarPessoa()) {
            t.definirPessoa(null, null);
        } else if (pessoa != null) {
            if (!t.isContaNosGastos()) {
                throw new RegraNegocioException(
                        "Só gastos têm dono. Esta transação é " + t.getTipoLancamento()
                                + " e não entra nos gastos.");
            }
            t.definirPessoa(pessoa, OrigemPessoa.MANUAL);
        }

        // Observação (texto vazio apaga)
        if (pedido.observacao() != null) {
            t.setObservacao(pedido.observacao().isBlank() ? null : pedido.observacao().trim());
        }
    }

    /**
     * Roda de novo as decisões automáticas. Elas pulam o que é MANUAL,
     * então só preenchem o que acabou de ser restaurado.
     */
    private void reaplicarAutomaticos(Transacao t, List<Regra> regras) {
        classificador.aplicar(t, t.getConta().getTipo());
        aplicadorLegenda.aplicar(t, regras);
        destinoPadrao.aplicar(t);
    }

    // ------------------------------------------------------------------
    // Validações
    // ------------------------------------------------------------------

    private void validarPedido(EdicaoTransacaoRequest pedido) {
        if (!pedido.temAlteracao()) {
            throw new RegraNegocioException("Nenhuma alteração informada.");
        }
        if (pedido.descricao() != null && pedido.querRestaurarDescricao()) {
            throw new RegraNegocioException("Informe a descrição OU restaurarDescricao, não os dois.");
        }
        if (pedido.categoria() != null && pedido.querRestaurarCategoria()) {
            throw new RegraNegocioException("Informe a categoria OU restaurarCategoria, não os dois.");
        }
        if (pedido.pessoaId() != null && pedido.querRestaurarPessoa()) {
            throw new RegraNegocioException("Informe a pessoaId OU restaurarPessoa, não os dois.");
        }
        if ((pedido.tipoLancamento() != null || pedido.contaNosGastos() != null)
                && pedido.querRestaurarClassificacao()) {
            throw new RegraNegocioException("Informe a classificação OU restaurarClassificacao, não os dois.");
        }
        if (pedido.tipoLancamento() == TipoLancamento.NAO_CLASSIFICADO) {
            throw new RegraNegocioException("NAO_CLASSIFICADO não pode ser escolhido à mão.");
        }
        if (pedido.descricao() != null && pedido.descricao().isBlank()) {
            throw new RegraNegocioException("A descrição não pode ficar vazia. Para voltar à do banco, use restaurarDescricao.");
        }
    }

    private Pessoa pessoaAtiva(Long id) {
        Pessoa pessoa = pessoaService.encontrar(id);
        if (!pessoa.isAtivo()) {
            throw new RegraNegocioException("A pessoa \"" + pessoa.getNome() + "\" está desativada.");
        }
        return pessoa;
    }
}
