package dev.alisonsantos.appgastos.classificacao;

import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.transacao.OrigemClassificacao;
import dev.alisonsantos.appgastos.transacao.StatusTransacao;
import dev.alisonsantos.appgastos.transacao.TipoLancamento;
import dev.alisonsantos.appgastos.transacao.TipoMovimento;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes das regras de classificação.
 *
 * Testes de unidade: não sobem o Spring nem usam banco de dados.
 * Rodam em milissegundos. Cada caso foi tirado dos dados reais da Pluggy.
 *
 * Estrutura de cada teste (o padrão "dado, quando, então"):
 *   - dado: monta a transação
 *   - quando: classifica
 *   - então: confere o resultado
 */
@DisplayName("Classificação dos lançamentos")
class ClassificadorLancamentoTest {

    private final ClassificadorLancamento classificador = new ClassificadorLancamento();

    private final Conta contaCartao = new Conta("Santander", "SANTANDER ELITE VISA", TipoConta.CARTAO_CREDITO);
    private final Conta contaCorrente = new Conta("Santander", "Banco Santander Pessoa Física", TipoConta.CORRENTE);

    // ------------------------------------------------------------------
    // Cartão de crédito
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("No cartão de crédito")
    class Cartao {

        @Test
        @DisplayName("Pagamento da fatura anterior não entra nos gastos")
        void pagamentoDeFatura() {
            Transacao t = transacao(contaCartao, "PAGAMENTO DE FATURA", "-7387.81", TipoMovimento.CREDITO);
            t.setOperacaoPluggy("PAGAMENTO");
            t.setCategoriaPluggy("Credit card payment");

            Classificacao resultado = classificador.classificar(t, TipoConta.CARTAO_CREDITO);

            assertThat(resultado.tipo()).isEqualTo(TipoLancamento.PAGAMENTO_FATURA);
            assertThat(resultado.contaNosGastos()).isFalse();
        }

        @Test
        @DisplayName("\"Pagamento recebido\" com operação PAGAMENTO_FATURA também é pagamento de fatura")
        void pagamentoRecebido() {
            Transacao t = transacao(contaCartao, "Pagamento recebido", "-554.95", TipoMovimento.CREDITO);
            t.setOperacaoPluggy("PAGAMENTO_FATURA");

            assertThat(classificador.classificar(t, TipoConta.CARTAO_CREDITO).tipo())
                    .isEqualTo(TipoLancamento.PAGAMENTO_FATURA);
        }

        @Test
        @DisplayName("Anuidade é tarifa e entra nos gastos")
        void anuidade() {
            Transacao t = transacao(contaCartao, "ANUIDADE DIFERENCIADA", "27.50", TipoMovimento.DEBITO);
            t.setOperacaoPluggy("TARIFA");

            Classificacao resultado = classificador.classificar(t, TipoConta.CARTAO_CREDITO);

            assertThat(resultado.tipo()).isEqualTo(TipoLancamento.TARIFA);
            assertThat(resultado.contaNosGastos()).isTrue();
        }

        @Test
        @DisplayName("Estorno de centavos (arredondamento de parcelas) é estorno e abate os gastos")
        void estornoDeCentavos() {
            Transacao t = transacao(contaCartao, "SHOPEE *ZEROGRAUVISEIR", "-0.02", TipoMovimento.CREDITO);

            Classificacao resultado = classificador.classificar(t, TipoConta.CARTAO_CREDITO);

            assertThat(resultado.tipo()).isEqualTo(TipoLancamento.ESTORNO);
            assertThat(resultado.contaNosGastos()).isTrue();
        }

        @Test
        @DisplayName("Débito comum no cartão é compra")
        void compra() {
            Transacao t = transacao(contaCartao, "DL*UberRides", "19.76", TipoMovimento.DEBITO);
            t.setOperacaoPluggy("PAGAMENTO");

            Classificacao resultado = classificador.classificar(t, TipoConta.CARTAO_CREDITO);

            assertThat(resultado.tipo()).isEqualTo(TipoLancamento.COMPRA);
            assertThat(resultado.contaNosGastos()).isTrue();
        }
    }

    // ------------------------------------------------------------------
    // Conta corrente
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Na conta corrente")
    class ContaCorrente {

        @Test
        @DisplayName("Transferência entre contas próprias não é gasto")
        void transferenciaPropria() {
            Transacao t = transacao(contaCorrente, "PIX ENVIADO Alison Souza Santos", "-500.00", TipoMovimento.DEBITO);
            t.setCategoriaPluggyId("04000000");

            Classificacao resultado = classificador.classificar(t, TipoConta.CORRENTE);

            assertThat(resultado.tipo()).isEqualTo(TipoLancamento.TRANSFERENCIA_PROPRIA);
            assertThat(resultado.contaNosGastos()).isFalse();
        }

        @Test
        @DisplayName("Pix para o banco do cartão é pagamento de fatura")
        void pixParaBancoEmissor() {
            Transacao t = transacao(contaCorrente, "PIX ENVIADO Banco Digio S A", "-44.29", TipoMovimento.DEBITO);
            t.setOperacaoPluggy("PIX");
            t.setContraparteNome("Banco Digio S A");

            Classificacao resultado = classificador.classificar(t, TipoConta.CORRENTE);

            assertThat(resultado.tipo()).isEqualTo(TipoLancamento.PAGAMENTO_FATURA);
            assertThat(resultado.contaNosGastos()).isFalse();
        }

        @Test
        @DisplayName("Pix para uma pessoa NÃO é pagamento de fatura")
        void pixParaPessoa() {
            Transacao t = transacao(contaCorrente, "PIX ENVIADO Vitor Hugo de Souza Silva", "-4.00", TipoMovimento.DEBITO);
            t.setOperacaoPluggy("PIX");
            t.setContraparteNome("Vitor Hugo de Souza Silva");

            assertThat(classificador.classificar(t, TipoConta.CORRENTE).tipo())
                    .isEqualTo(TipoLancamento.OUTRO);
        }

        @Test
        @DisplayName("Conta de água paga pela conta corrente fica como \"outro\", para revisão")
        void contaDeAgua() {
            Transacao t = transacao(contaCorrente,
                    "CONTA DE AGUA E ESGOTO EM CANAIS INTERNET SANEPAR PARANA", "-89.43", TipoMovimento.DEBITO);
            t.setOperacaoPluggy("CONVENIO_ARRECADACAO");

            Classificacao resultado = classificador.classificar(t, TipoConta.CORRENTE);

            assertThat(resultado.tipo()).isEqualTo(TipoLancamento.OUTRO);
            assertThat(resultado.contaNosGastos()).isTrue();
        }

        @Test
        @DisplayName("Dinheiro que entra é recebimento e não entra nos gastos")
        void recebimento() {
            Transacao t = transacao(contaCorrente, "PIX RECEBIDO Maria da Silva", "1072.27", TipoMovimento.CREDITO);
            t.setOperacaoPluggy("PIX");

            Classificacao resultado = classificador.classificar(t, TipoConta.CORRENTE);

            assertThat(resultado.tipo()).isEqualTo(TipoLancamento.RECEBIMENTO);
            assertThat(resultado.contaNosGastos()).isFalse();
        }
    }

    // ------------------------------------------------------------------
    // Classificação manual
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Classificação corrigida à mão nunca é sobrescrita")
    void classificacaoManualPreservada() {
        Transacao t = transacao(contaCartao, "DL*UberRides", "19.76", TipoMovimento.DEBITO);
        t.definirClassificacao(TipoLancamento.OUTRO, false, OrigemClassificacao.MANUAL);

        boolean mudou = classificador.aplicar(t, TipoConta.CARTAO_CREDITO);

        assertThat(mudou).isFalse();
        assertThat(t.getTipoLancamento()).isEqualTo(TipoLancamento.OUTRO);
        assertThat(t.isContaNosGastos()).isFalse();
        assertThat(t.getOrigemClassificacao()).isEqualTo(OrigemClassificacao.MANUAL);
    }

    @Test
    @DisplayName("Aplicar a mesma regra duas vezes não muda nada na segunda")
    void aplicarDuasVezes() {
        Transacao t = transacao(contaCartao, "DL*UberRides", "19.76", TipoMovimento.DEBITO);

        assertThat(classificador.aplicar(t, TipoConta.CARTAO_CREDITO)).isTrue();
        assertThat(classificador.aplicar(t, TipoConta.CARTAO_CREDITO)).isFalse();
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Transacao transacao(Conta conta, String descricao, String valor, TipoMovimento movimento) {
        return new Transacao(
                "pluggy-" + descricao.hashCode(),
                conta,
                Instant.parse("2026-11-01T12:00:00Z"),
                new BigDecimal(valor),
                descricao,
                movimento,
                StatusTransacao.CONFIRMADA);
    }
}
