package dev.alisonsantos.appgastos.legenda;

import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.divisao.DivisaoSalva;
import dev.alisonsantos.appgastos.divisao.ModoDivisao;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.TipoPessoa;
import dev.alisonsantos.appgastos.transacao.OrigemPessoa;
import dev.alisonsantos.appgastos.transacao.StatusTransacao;
import dev.alisonsantos.appgastos.transacao.TipoMovimento;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes das regras que DIVIDEM a compra (com uma divisão salva).
 */
@DisplayName("Legenda: regras com divisão salva")
class AplicadorLegendaDivisaoTest {

    private final AplicadorLegenda aplicador = new AplicadorLegenda(new MotorLegenda());
    private final Conta conta = new Conta("Santander", "Cartão Santander", TipoConta.CARTAO_CREDITO);

    private final List<Pessoa> familia = List.of(
            new Pessoa("Alison", TipoPessoa.PESSOA),
            new Pessoa("Gabriella", TipoPessoa.PESSOA),
            new Pessoa("Lorena", TipoPessoa.PESSOA),
            new Pessoa("Paulo", TipoPessoa.PESSOA),
            new Pessoa("Guilherme", TipoPessoa.PESSOA));

    @Test
    @DisplayName("A regra do Spotify divide a compra em 5 partes de R$ 8,18")
    void regraDivide() {
        Transacao spotify = spotify();

        boolean mudou = aplicador.aplicar(spotify, List.of(regraSpotify()));

        assertThat(mudou).isTrue();
        assertThat(spotify.isDividida()).isTrue();
        assertThat(spotify.getOrigemPessoa()).isEqualTo(OrigemPessoa.REGRA);
        assertThat(spotify.getRateios()).hasSize(5)
                .allSatisfy(r -> assertThat(r.getValor()).isEqualByComparingTo("8.18"));
    }

    @Test
    @DisplayName("Reaplicar a mesma regra não refaz a divisão (sem apagar e recriar os rateios)")
    void reaplicarNaoRefaz() {
        Transacao spotify = spotify();
        Regra regra = regraSpotify();
        aplicador.aplicar(spotify, List.of(regra));

        boolean mudouDeNovo = aplicador.aplicar(spotify, List.of(regra));

        assertThat(mudouDeNovo).isFalse();
    }

    @Test
    @DisplayName("Se a regra deixa de valer, a divisão feita por ela é desfeita")
    void regraDesativadaDesfaz() {
        Transacao spotify = spotify();
        Regra regra = regraSpotify();
        aplicador.aplicar(spotify, List.of(regra));

        regra.setAtiva(false);
        aplicador.aplicar(spotify, List.of(regra));

        assertThat(spotify.isDividida()).isFalse();
        assertThat(spotify.getOrigemPessoa()).isNull();
    }

    @Test
    @DisplayName("Uma divisão feita à mão nunca é substituída pela regra")
    void divisaoManualPreservada() {
        Transacao spotify = spotify();
        spotify.definirPessoa(familia.getFirst(), OrigemPessoa.MANUAL);

        aplicador.aplicar(spotify, List.of(regraSpotify()));

        assertThat(spotify.isDividida()).isFalse();
        assertThat(spotify.getPessoa()).isSameAs(familia.getFirst());
    }

    private Regra regraSpotify() {
        DivisaoSalva spotifyFamilia = new DivisaoSalva("Spotify Família", ModoDivisao.IGUAL);
        spotifyFamilia.definirPartes(familia.stream()
                .map(p -> new DivisaoSalva.Integrante(p, null))
                .toList());

        Regra regra = new Regra(TipoChave.DESCRICAO_CONTEM, "SPOTIFY");
        regra.setDescricao("Spotify Família");
        regra.setDivisaoSalva(spotifyFamilia);
        return regra;
    }

    private Transacao spotify() {
        return new Transacao("pluggy-spotify", conta, Instant.parse("2026-11-06T08:39:19Z"),
                new BigDecimal("40.90"), "EBN *SPOTIFY", TipoMovimento.DEBITO, StatusTransacao.CONFIRMADA);
    }
}
