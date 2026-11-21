package dev.alisonsantos.appgastos.sync;

import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyCreditCardMetadata;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyParticipante;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyPaymentData;
import dev.alisonsantos.appgastos.pluggy.dto.PluggyTransaction;
import dev.alisonsantos.appgastos.transacao.StatusTransacao;
import dev.alisonsantos.appgastos.transacao.TipoMovimento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes da conversão dos dados da Pluggy para o formato do app.
 */
@DisplayName("Conversão dos dados da Pluggy")
class PluggyConversorTest {

    @Test
    @DisplayName("Tira os espaços do fim do nome (como vem o cartão do Santander)")
    void textoSemEspacos() {
        assertThat(PluggyConversor.texto("SANTANDER ELITE VISA          ", 100))
                .isEqualTo("SANTANDER ELITE VISA");
    }

    @Test
    @DisplayName("Texto vazio ou só com espaços vira null")
    void textoVazio() {
        assertThat(PluggyConversor.texto("   ", 100)).isNull();
        assertThat(PluggyConversor.texto(null, 100)).isNull();
    }

    @Test
    @DisplayName("Corta o texto no tamanho da coluna")
    void textoCortado() {
        assertThat(PluggyConversor.texto("ABCDEFGHIJ", 4)).isEqualTo("ABCD");
    }

    /**
     * @ParameterizedTest: o mesmo teste roda uma vez para cada linha do CsvSource.
     * Primeira coluna: a entrada. Segunda: o resultado esperado.
     */
    @ParameterizedTest(name = "CNPJ \"{0}\" vira \"{1}\"")
    @CsvSource(value = {
            "06.057.223/0001-71 | 06057223000171",
            "06057223000171     | 06057223000171",
            "''                 | ",
            "123                | "
    }, delimiter = '|')
    void cnpj(String entrada, String esperado) {
        assertThat(PluggyConversor.cnpj(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Converte tipo de conta, movimento e status da Pluggy")
    void conversoesSimples() {
        assertThat(PluggyConversor.tipoConta("CREDIT")).isEqualTo(TipoConta.CARTAO_CREDITO);
        assertThat(PluggyConversor.tipoConta("BANK")).isEqualTo(TipoConta.CORRENTE);
        assertThat(PluggyConversor.tipoMovimento("CREDIT")).isEqualTo(TipoMovimento.CREDITO);
        assertThat(PluggyConversor.tipoMovimento("DEBIT")).isEqualTo(TipoMovimento.DEBITO);
        assertThat(PluggyConversor.status("PENDING")).isEqualTo(StatusTransacao.PENDENTE);
        assertThat(PluggyConversor.status("POSTED")).isEqualTo(StatusTransacao.CONFIRMADA);
    }

    @Test
    @DisplayName("Data de fechamento da fatura vira a data correta (sem virar o dia anterior por fuso)")
    void data() {
        assertThat(PluggyConversor.data(Instant.parse("2026-10-30T00:00:00Z")))
                .isEqualTo(LocalDate.of(2026, 10, 30));
    }

    @Test
    @DisplayName("Lançamento com billForecastDate \"0001-01\" é projeção espúria")
    void projecaoEspuria() {
        assertThat(PluggyConversor.isProjecaoEspuria(transacaoCartao("0001-01", "4948"))).isTrue();
        assertThat(PluggyConversor.isProjecaoEspuria(transacaoCartao("2026-11", "4948"))).isFalse();
    }

    @Test
    @DisplayName("Final do cartão só é aceito com exatamente 4 dígitos")
    void finalCartao() {
        assertThat(PluggyConversor.ultimosDigitosCartao(metadados("2026-11", "4948"))).isEqualTo("4948");
        assertThat(PluggyConversor.ultimosDigitosCartao(metadados("2026-11", "12345"))).isNull();
        assertThat(PluggyConversor.ultimosDigitosCartao(metadados("2026-11", "ab12"))).isNull();
    }

    @Test
    @DisplayName("No Pix recebido, a contraparte é quem pagou; no enviado, quem recebeu")
    void contraparte() {
        PluggyPaymentData pagamento = new PluggyPaymentData(
                new PluggyParticipante("Quem Pagou"), new PluggyParticipante("Quem Recebeu"), "PIX");

        assertThat(PluggyConversor.contraparte(transacaoPix("CREDIT", pagamento))).isEqualTo("Quem Pagou");
        assertThat(PluggyConversor.contraparte(transacaoPix("DEBIT", pagamento))).isEqualTo("Quem Recebeu");
    }

    // ------------------------------------------------------------------
    // Apoio: monta os records da Pluggy com só o que cada teste precisa
    // ------------------------------------------------------------------

    private PluggyCreditCardMetadata metadados(String previsaoFatura, String finalCartao) {
        return new PluggyCreditCardMetadata(finalCartao, 5411, null, "SINGLE", null, null,
                null, null, null, previsaoFatura, null);
    }

    private PluggyTransaction transacaoCartao(String previsaoFatura, String finalCartao) {
        return new PluggyTransaction("id", "conta", "COMPRA", "COMPRA", "BRL", new BigDecimal("10.00"),
                Instant.parse("2026-11-01T12:00:00Z"), null, null, "POSTED", "DEBIT", "PAGAMENTO",
                metadados(previsaoFatura, finalCartao), null, null, null, null);
    }

    private PluggyTransaction transacaoPix(String tipo, PluggyPaymentData pagamento) {
        return new PluggyTransaction("id", "conta", "PIX", "PIX", "BRL", new BigDecimal("10.00"),
                Instant.parse("2026-11-01T12:00:00Z"), null, null, "POSTED", tipo, "PIX",
                null, null, pagamento, null, null);
    }
}
