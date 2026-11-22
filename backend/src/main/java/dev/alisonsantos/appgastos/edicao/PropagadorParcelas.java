package dev.alisonsantos.appgastos.edicao;

import dev.alisonsantos.appgastos.transacao.OrigemClassificacao;
import dev.alisonsantos.appgastos.transacao.OrigemDescricao;
import dev.alisonsantos.appgastos.transacao.OrigemPessoa;
import dev.alisonsantos.appgastos.transacao.Transacao;
import dev.alisonsantos.appgastos.transacao.TransacaoRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Cuida das parcelas da mesma compra.
 *
 * Uma compra em 12x vira 12 transações, uma por mês. Se você corrigir a
 * parcela 1 ("essa é da Marlene"), as outras 11 têm que acompanhar,
 * inclusive as que ainda nem chegaram: quando a parcela 2 vier no sync
 * do mês que vem, ela herda a edição da irmã.
 */
@Component
public class PropagadorParcelas {

    private final TransacaoRepository transacaoRepository;

    public PropagadorParcelas(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    /**
     * Todas as parcelas da mesma compra, incluindo a própria transação.
     * Se não for parcelada (ou faltar algum dado para agrupar), devolve só ela.
     */
    public List<Transacao> parcelasDaMesmaCompra(Transacao transacao) {
        if (!podeAgrupar(transacao)) {
            return List.of(transacao);
        }
        List<Transacao> parcelas = transacaoRepository.findParcelasDaMesmaCompra(
                transacao.getCartao().getId(),
                transacao.getDataCompra(),
                transacao.getParcelaTotal(),
                transacao.getDescricaoBanco());
        if (parcelas.contains(transacao)) {
            return parcelas;
        }
        List<Transacao> comAPropria = new ArrayList<>(parcelas);
        comAPropria.add(transacao);
        return comAPropria;
    }

    /**
     * Usado pelo sync quando chega uma parcela nova: se alguma irmã já foi
     * editada à mão (ou revisada), a nova copia as edições dela.
     */
    public void herdarEdicoes(Transacao nova) {
        if (!podeAgrupar(nova)) {
            return;
        }
        Optional<Transacao> irmaEditada = transacaoRepository.findParcelasDaMesmaCompra(
                        nova.getCartao().getId(),
                        nova.getDataCompra(),
                        nova.getParcelaTotal(),
                        nova.getDescricaoBanco())
                .stream()
                .filter(irma -> irma != nova && (irma.temEdicaoManual() || irma.isRevisada()))
                // A edição mais recente é a que vale
                .max(Comparator.comparing(Transacao::getAtualizadoEm,
                        Comparator.nullsFirst(Comparator.naturalOrder())));

        irmaEditada.ifPresent(irma -> copiarEdicoes(irma, nova));
    }

    /**
     * Copia para o destino tudo o que foi editado à mão na origem.
     * O que não foi editado à mão continua sendo decidido pelas regras.
     */
    public static void copiarEdicoes(Transacao origem, Transacao destino) {
        if (origem.getOrigemDescricao() == OrigemDescricao.MANUAL) {
            destino.definirDescricao(origem.getDescricao(), OrigemDescricao.MANUAL);
        }
        if (origem.isCategoriaManual()) {
            destino.setCategoria(origem.getCategoria());
            destino.setCategoriaManual(true);
        }
        if (origem.isClassificacaoManual()) {
            destino.definirClassificacao(origem.getTipoLancamento(), origem.isContaNosGastos(),
                    OrigemClassificacao.MANUAL);
        }
        if (origem.getOrigemPessoa() == OrigemPessoa.MANUAL) {
            destino.definirPessoa(origem.getPessoa(), OrigemPessoa.MANUAL);
        }
        if (destino.getObservacao() == null && origem.getObservacao() != null) {
            destino.setObservacao(origem.getObservacao());
        }
        // Compra já revisada: a parcela nova também não precisa de revisão
        if (origem.isRevisada()) {
            destino.marcarRevisada();
        }
    }

    /** Para agrupar parcelas, precisa ser parcelada e ter cartão e data da compra. */
    private boolean podeAgrupar(Transacao transacao) {
        return transacao.isParcelada()
                && transacao.getCartao() != null
                && transacao.getCartao().getId() != null
                && transacao.getDataCompra() != null;
    }
}
