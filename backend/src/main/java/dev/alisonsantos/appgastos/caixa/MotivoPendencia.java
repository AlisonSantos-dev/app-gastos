package dev.alisonsantos.appgastos.caixa;

import dev.alisonsantos.appgastos.transacao.OrigemDescricao;
import dev.alisonsantos.appgastos.transacao.Transacao;

import java.util.ArrayList;
import java.util.List;

/**
 * Por que uma compra está na caixa de entrada.
 */
public enum MotivoPendencia {

    /** Ninguém é dono: cartão compartilhado (modo PERGUNTAR) ou conta sem dono. */
    SEM_DONO,

    /** Nenhuma regra da legenda reconheceu a descrição, e ela não foi editada à mão. */
    SEM_LEGENDA;

    /**
     * Os motivos de uma transação estar pendente. Lista vazia = não está pendente.
     * Só gastos não revisados podem estar na caixa.
     */
    public static List<MotivoPendencia> de(Transacao transacao) {
        List<MotivoPendencia> motivos = new ArrayList<>();
        if (!transacao.isContaNosGastos() || transacao.isRevisada()) {
            return motivos;
        }
        if (transacao.getPessoa() == null) {
            motivos.add(SEM_DONO);
        }
        if (transacao.getRegra() == null && transacao.getOrigemDescricao() != OrigemDescricao.MANUAL) {
            motivos.add(SEM_LEGENDA);
        }
        return motivos;
    }
}
