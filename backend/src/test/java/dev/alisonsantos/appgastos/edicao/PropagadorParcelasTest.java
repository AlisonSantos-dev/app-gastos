package dev.alisonsantos.appgastos.edicao;

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
 * Testes da cópia das edições entre parcelas da mesma compra.
 */
@DisplayName("Edições entre parcelas")
class PropagadorParcelasTest {

    private final Conta conta = new Conta("Santander", "Cartão Santander", TipoConta.CARTAO_CREDITO);
    private final Pessoa marlene = new Pessoa("Marlene", TipoPessoa.PESSOA);

    @Test
    @DisplayName("A parcela nova herda tudo o que foi editado à mão na irmã")
    void herdaEdicoesManuais() {
        Transacao parcela1 = parcela(1);
        parcela1.definirDescricao("Sofá da sala", OrigemDescricao.MANUAL);
        parcela1.setCategoria("Casa");
        parcela1.setCategoriaManual(true);
        parcela1.definirPessoa(marlene, OrigemPessoa.MANUAL);
        parcela1.setObservacao("Dividido com a Marlene");

        Transacao parcela2 = parcela(2);
        PropagadorParcelas.copiarEdicoes(parcela1, parcela2);

        assertThat(parcela2.getDescricao()).isEqualTo("Sofá da sala");
        assertThat(parcela2.getOrigemDescricao()).isEqualTo(OrigemDescricao.MANUAL);
        assertThat(parcela2.getCategoria()).isEqualTo("Casa");
        assertThat(parcela2.isCategoriaManual()).isTrue();
        assertThat(parcela2.getPessoa()).isSameAs(marlene);
        assertThat(parcela2.getOrigemPessoa()).isEqualTo(OrigemPessoa.MANUAL);
        assertThat(parcela2.getObservacao()).isEqualTo("Dividido com a Marlene");
    }

    @Test
    @DisplayName("O que não foi editado à mão não é copiado (continua com as regras)")
    void naoCopiaOqueEhAutomatico() {
        Transacao parcela1 = parcela(1);
        parcela1.definirDescricao("Nome dado por regra", OrigemDescricao.REGRA);
        parcela1.definirPessoa(marlene, OrigemPessoa.CARTAO);

        Transacao parcela2 = parcela(2);
        PropagadorParcelas.copiarEdicoes(parcela1, parcela2);

        assertThat(parcela2.getDescricao()).isNull();
        assertThat(parcela2.getOrigemDescricao()).isEqualTo(OrigemDescricao.BANCO);
        assertThat(parcela2.getPessoa()).isNull();
    }

    @Test
    @DisplayName("A classificação manual também passa para a parcela nova")
    void copiaClassificacaoManual() {
        Transacao parcela1 = parcela(1);
        parcela1.definirClassificacao(TipoLancamento.OUTRO, false, OrigemClassificacao.MANUAL);

        Transacao parcela2 = parcela(2);
        PropagadorParcelas.copiarEdicoes(parcela1, parcela2);

        assertThat(parcela2.getTipoLancamento()).isEqualTo(TipoLancamento.OUTRO);
        assertThat(parcela2.isContaNosGastos()).isFalse();
        assertThat(parcela2.isClassificacaoManual()).isTrue();
    }

    private Transacao parcela(int numero) {
        Transacao t = new Transacao("pluggy-adonai-" + numero, conta,
                Instant.parse("2026-10-25T16:12:07Z").plusSeconds(numero * 2_592_000L),
                new BigDecimal("282.49"), "ADONAI IND E COM DE ES",
                TipoMovimento.DEBITO, StatusTransacao.CONFIRMADA);
        t.setDataCompra(Instant.parse("2026-10-25T16:12:07Z"));
        t.setParcela((short) numero, (short) 12);
        return t;
    }
}
