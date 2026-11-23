package dev.alisonsantos.appgastos.legenda;

import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.transacao.OrigemDescricao;
import dev.alisonsantos.appgastos.transacao.OrigemPessoa;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Aplica na transação o que o MotorLegenda decidiu.
 *
 * Respeita a cascata de quem manda em cada atributo:
 *   - Descrição: MANUAL > REGRA > BANCO
 *   - Categoria: MANUAL > REGRA
 *   - Pessoa:    MANUAL > REGRA > CARTÃO / CONTA (o DestinoPadrao, que roda depois)
 *
 * E se uma regra deixar de valer (foi desativada ou mudou), o que ela
 * tinha definido é desfeito: a descrição volta a ser a do banco, e o
 * dono volta a ser decidido pelo cartão.
 */
@Component
public class AplicadorLegenda {

    private final MotorLegenda motor;

    public AplicadorLegenda(MotorLegenda motor) {
        this.motor = motor;
    }

    /**
     * @return true se alguma coisa mudou na transação
     */
    public boolean aplicar(Transacao transacao, List<Regra> regras) {
        ResultadoLegenda resultado = motor.avaliar(transacao, regras);
        boolean mudou = false;

        // ---------------- Descrição ----------------
        if (transacao.getOrigemDescricao() != OrigemDescricao.MANUAL) {
            if (resultado.regraDescricao() != null) {
                String nova = resultado.regraDescricao().getDescricao();
                if (!nova.equals(transacao.getDescricao())
                        || transacao.getOrigemDescricao() != OrigemDescricao.REGRA) {
                    transacao.definirDescricao(nova, OrigemDescricao.REGRA);
                    mudou = true;
                }
            } else if (transacao.getOrigemDescricao() == OrigemDescricao.REGRA) {
                // A regra que dava o nome não vale mais: volta para a do banco
                transacao.definirDescricao(null, OrigemDescricao.BANCO);
                mudou = true;
            }
        }

        // ---------------- Categoria ----------------
        // A categoria editada à mão nunca é tocada
        if (!transacao.isCategoriaManual()) {
            String novaCategoria = resultado.regraCategoria() == null ? null
                    : resultado.regraCategoria().getCategoria();
            if (!Objects.equals(novaCategoria, transacao.getCategoria())) {
                transacao.setCategoria(novaCategoria);
                mudou = true;
            }
        }

        // ---------------- Pessoa ----------------
        // Só gasto tem dono, e a escolha manual (ou a divisão) nunca é tocada
        if (transacao.getOrigemPessoa() != OrigemPessoa.MANUAL
                && transacao.getOrigemPessoa() != OrigemPessoa.DIVISAO) {
            Pessoa novaPessoa = transacao.isContaNosGastos() && resultado.regraPessoa() != null
                    ? resultado.regraPessoa().getPessoa()
                    : null;

            if (novaPessoa != null) {
                if (!Objects.equals(novaPessoa, transacao.getPessoa())
                        || transacao.getOrigemPessoa() != OrigemPessoa.REGRA) {
                    transacao.definirPessoa(novaPessoa, OrigemPessoa.REGRA);
                    mudou = true;
                }
            } else if (transacao.getOrigemPessoa() == OrigemPessoa.REGRA) {
                // A regra que definia o dono não vale mais: o DestinoPadrao
                // (que roda logo depois) decide de novo pelo cartão ou conta
                transacao.definirPessoa(null, null);
                mudou = true;
            }
        }

        // ---------------- Qual regra identificou ----------------
        Regra principal = resultado.regraPrincipal();
        if (!Objects.equals(principal, transacao.getRegra())) {
            transacao.setRegra(principal);
            mudou = true;
        }

        return mudou;
    }
}
