package dev.alisonsantos.appgastos.destino;

import dev.alisonsantos.appgastos.cartao.Cartao;
import dev.alisonsantos.appgastos.cartao.ModoDono;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.transacao.OrigemPessoa;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Define o destino PADRÃO de uma transação (de quem ela é),
 * a partir do cartão ou da conta.
 *
 * É o primeiro degrau da cascata de destino:
 *   MANUAL ou DIVISÃO (você escolheu)  >  REGRA  >  CARTÃO / CONTA (aqui)
 *
 * Por isso, se a transação já tiver um destino MANUAL, de DIVISÃO ou de REGRA,
 * nada é alterado.
 */
@Component
public class DestinoPadrao {

    /**
     * @return true se o destino mudou
     */
    public boolean aplicar(Transacao transacao) {
        OrigemPessoa origemAtual = transacao.getOrigemPessoa();
        if (origemAtual == OrigemPessoa.MANUAL || origemAtual == OrigemPessoa.REGRA
                || origemAtual == OrigemPessoa.DIVISAO) {
            return false;
        }

        Pessoa novoDono = null;
        OrigemPessoa novaOrigem = null;

        // Só o que é gasto tem dono. Pagamento de fatura, transferência
        // própria e recebimento ficam sem destino.
        if (transacao.isContaNosGastos()) {
            Cartao cartao = transacao.getCartao();
            if (cartao != null) {
                // Cartão FIXO: vai para o dono. Cartão PERGUNTAR: fica sem
                // dono, para você escolher na caixa de entrada.
                if (cartao.getModoDono() == ModoDono.FIXO && cartao.getDonoPadrao() != null) {
                    novoDono = cartao.getDonoPadrao();
                    novaOrigem = OrigemPessoa.CARTAO;
                }
            } else if (transacao.getConta().getDonoPadrao() != null) {
                // Sem cartão (ex.: débito na conta corrente): vai para o dono da conta
                novoDono = transacao.getConta().getDonoPadrao();
                novaOrigem = OrigemPessoa.CONTA;
            }
        }

        boolean mudou = !Objects.equals(transacao.getPessoa(), novoDono)
                || transacao.getOrigemPessoa() != novaOrigem;
        if (mudou) {
            transacao.definirPessoa(novoDono, novaOrigem);
        }
        return mudou;
    }
}
