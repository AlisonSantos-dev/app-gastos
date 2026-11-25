package dev.alisonsantos.appgastos.recebimento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes das regras dos recebimentos: o nome no Pix e a situação de cada pessoa.
 */
@DisplayName("Recebimentos")
class RecebimentoRegrasTest {

    @Test
    @DisplayName("O nome cortado pelo banco ainda reconhece a pessoa")
    void nomeCortado() {
        assertThat(NomeNoExtrato.bate("MARLENE APARECIDA DOS SAN", "Marlene Aparecida dos Santos")).isTrue();
        assertThat(NomeNoExtrato.bate("Nelson Ferreira dos Santos", "NELSON FERREIRA")).isTrue();
    }

    @Test
    @DisplayName("Nomes diferentes, ou curtos demais, não batem")
    void nomesQueNaoBatem() {
        assertThat(NomeNoExtrato.bate("MARIA DA SILVA", "Marlene Aparecida dos Santos")).isFalse();
        assertThat(NomeNoExtrato.bate("ANA", "ANA PAULA SOUZA")).isFalse();
        assertThat(NomeNoExtrato.bate(null, "Marlene Aparecida")).isFalse();
    }

    @Test
    @DisplayName("Situação: pendente, parcial, quitado (com tolerância de R$ 1,00) e mês não publicado")
    void situacao() {
        assertThat(SituacaoRecebimento.de(null, BigDecimal.ZERO)).isEqualTo(SituacaoRecebimento.MES_NAO_PUBLICADO);
        assertThat(SituacaoRecebimento.de(BigDecimal.ZERO, BigDecimal.ZERO)).isEqualTo(SituacaoRecebimento.SEM_DEBITO);
        assertThat(SituacaoRecebimento.de(dinheiro("2118.14"), BigDecimal.ZERO)).isEqualTo(SituacaoRecebimento.PENDENTE);
        assertThat(SituacaoRecebimento.de(dinheiro("2118.14"), dinheiro("1000.00"))).isEqualTo(SituacaoRecebimento.PARCIAL);
        assertThat(SituacaoRecebimento.de(dinheiro("2118.14"), dinheiro("2118.00"))).isEqualTo(SituacaoRecebimento.QUITADO);
        assertThat(SituacaoRecebimento.de(dinheiro("2118.14"), dinheiro("2200.00"))).isEqualTo(SituacaoRecebimento.QUITADO);
    }

    private BigDecimal dinheiro(String valor) {
        return new BigDecimal(valor);
    }
}
