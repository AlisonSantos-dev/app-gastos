package dev.alisonsantos.appgastos.caixa;

import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.TipoPessoa;
import dev.alisonsantos.appgastos.transacao.OrigemClassificacao;
import dev.alisonsantos.appgastos.transacao.OrigemDescricao;
import dev.alisonsantos.appgastos.transacao.OrigemPessoa;
import dev.alisonsantos.appgastos.transacao.StatusTransacao;
import dev.alisonsantos.appgastos.transacao.TipoLancamento;
import dev.alisonsantos.appgastos.transacao.TipoMovimento;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes das regras que colocam uma compra na caixa de entrada.
 */
@DisplayName("Caixa de entrada: motivos de pendência")
class MotivoPendenciaTest {

    private final Conta conta = new Conta("Nubank", "Cartão Nubank", TipoConta.CARTAO_CREDITO);
    private final Pessoa marlene = new Pessoa("Marlene", TipoPessoa.PESSOA);

    @Test
    @DisplayName("Compra do cartão compartilhado, sem dono e sem legenda: os dois motivos")
    void semDonoESemLegenda() {
        assertThat(MotivoPendencia.de(compra("DL*UberRides")))
                .containsExactly(MotivoPendencia.SEM_DONO, MotivoPendencia.SEM_LEGENDA);
    }

    @Test
    @DisplayName("Com dono mas sem legenda: só SEM_LEGENDA")
    void soSemLegenda() {
        Transacao t = compra("VwConveniencia");
        t.definirPessoa(marlene, OrigemPessoa.CARTAO);

        assertThat(MotivoPendencia.de(t)).containsExactly(MotivoPendencia.SEM_LEGENDA);
    }

    @Test
    @DisplayName("Descrição editada à mão conta como legenda")
    void descricaoManual() {
        Transacao t = compra("ADONAI IND E COM DE ES");
        t.definirPessoa(marlene, OrigemPessoa.MANUAL);
        t.definirDescricao("Mesa de jantar", OrigemDescricao.MANUAL);

        assertThat(MotivoPendencia.de(t)).isEmpty();
    }

    @Test
    @DisplayName("Compra revisada sai da caixa, mesmo sem legenda")
    void revisada() {
        Transacao t = compra("VwConveniencia");
        t.definirPessoa(marlene, OrigemPessoa.CARTAO);
        t.marcarRevisada();

        assertThat(MotivoPendencia.de(t)).isEmpty();
    }

    @Test
    @DisplayName("O que não é gasto nunca vai para a caixa")
    void naoGasto() {
        Transacao t = compra("PAGAMENTO DE FATURA");
        t.definirClassificacao(TipoLancamento.PAGAMENTO_FATURA, false, OrigemClassificacao.AUTOMATICA);

        assertThat(MotivoPendencia.de(t)).isEmpty();
    }

    private Transacao compra(String descricao) {
        return new Transacao("pluggy-" + descricao.hashCode(), conta, Instant.parse("2026-11-15T12:00:00Z"),
                new BigDecimal("20.00"), descricao, TipoMovimento.DEBITO, StatusTransacao.CONFIRMADA);
    }
}
