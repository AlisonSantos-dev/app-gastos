package dev.alisonsantos.appgastos.legenda;

import dev.alisonsantos.appgastos.comum.RegraNegocioException;
import dev.alisonsantos.appgastos.divisao.CalculadoraDivisao;
import dev.alisonsantos.appgastos.divisao.DivisaoSalva;
import dev.alisonsantos.appgastos.divisao.ParteCalculada;
import dev.alisonsantos.appgastos.divisao.Rateio;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.transacao.OrigemDescricao;
import dev.alisonsantos.appgastos.transacao.OrigemPessoa;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aplica na transação o que o MotorLegenda decidiu.
 *
 * Respeita a cascata de quem manda em cada atributo:
 *   - Descrição: MANUAL > REGRA > BANCO
 *   - Categoria: MANUAL > REGRA
 *   - Pessoa:    MANUAL / DIVISÃO > REGRA (dono ou divisão salva) > CARTÃO / CONTA
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

        // ---------------- Pessoa (ou divisão) ----------------
        // Só gasto tem dono, e a escolha manual (ou a divisão manual) nunca é tocada
        if (transacao.getOrigemPessoa() != OrigemPessoa.MANUAL
                && transacao.getOrigemPessoa() != OrigemPessoa.DIVISAO) {
            Regra regraDestino = transacao.isContaNosGastos() ? resultado.regraPessoa() : null;

            if (regraDestino != null && regraDestino.getDivisaoSalva() != null) {
                // A regra DIVIDE a compra
                mudou |= aplicarDivisaoDaRegra(transacao, regraDestino.getDivisaoSalva());

            } else if (regraDestino != null) {
                // A regra dá UM dono
                Pessoa novaPessoa = regraDestino.getPessoa();
                if (!Objects.equals(novaPessoa, transacao.getPessoa())
                        || transacao.getOrigemPessoa() != OrigemPessoa.REGRA
                        || transacao.isDividida()) {
                    transacao.definirPessoa(novaPessoa, OrigemPessoa.REGRA);
                    mudou = true;
                }

            } else if (transacao.getOrigemPessoa() == OrigemPessoa.REGRA) {
                // A regra que definia o dono (ou a divisão) não vale mais: o
                // DestinoPadrao (que roda logo depois) decide de novo pelo cartão
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

    /**
     * Divide a compra com a divisão salva da regra. Só refaz se o resultado
     * for diferente do atual: sem isso, cada reaplicação da legenda apagaria
     * e recriaria os rateios de todas as compras divididas.
     *
     * @return true se a divisão mudou
     */
    private boolean aplicarDivisaoDaRegra(Transacao transacao, DivisaoSalva divisao) {
        if (transacao.getValor().signum() == 0) {
            return false;
        }
        List<ParteCalculada> partes;
        try {
            partes = CalculadoraDivisao.calcular(divisao.getModo(), divisao.paraEntradas(), transacao.getValor());
        } catch (RegraNegocioException e) {
            return false;
        }
        if (transacao.getOrigemPessoa() == OrigemPessoa.REGRA && mesmasPartes(transacao.getRateios(), partes)) {
            return false;
        }
        transacao.aplicarDivisao(partes, OrigemPessoa.REGRA);
        return true;
    }

    /** As mesmas pessoas, com os mesmos valores? */
    private boolean mesmasPartes(List<Rateio> atuais, List<ParteCalculada> novas) {
        if (atuais.size() != novas.size()) {
            return false;
        }
        Map<Object, BigDecimal> porPessoa = new HashMap<>();
        atuais.forEach(r -> porPessoa.put(chave(r.getPessoa()), r.getValor()));
        return novas.stream().allMatch(p -> {
            BigDecimal valorAtual = porPessoa.get(chave(p.pessoa()));
            return valorAtual != null && valorAtual.compareTo(p.valor()) == 0;
        });
    }

    /** Pelo id (que funciona até com os "proxies" do Hibernate); sem id, a própria instância. */
    private Object chave(Pessoa pessoa) {
        return pessoa.getId() != null ? pessoa.getId() : pessoa;
    }
}
