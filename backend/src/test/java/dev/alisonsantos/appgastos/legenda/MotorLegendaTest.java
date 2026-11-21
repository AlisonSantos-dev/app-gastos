package dev.alisonsantos.appgastos.legenda;

import dev.alisonsantos.appgastos.cartao.Cartao;
import dev.alisonsantos.appgastos.conta.Conta;
import dev.alisonsantos.appgastos.conta.TipoConta;
import dev.alisonsantos.appgastos.pessoa.Pessoa;
import dev.alisonsantos.appgastos.pessoa.TipoPessoa;
import dev.alisonsantos.appgastos.transacao.StatusTransacao;
import dev.alisonsantos.appgastos.transacao.TipoMovimento;
import dev.alisonsantos.appgastos.transacao.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes da cascata da legenda, com casos tirados dos dados reais.
 */
@DisplayName("Legenda em cascata")
class MotorLegendaTest {

    private final MotorLegenda motor = new MotorLegenda();

    private final Conta contaCartao = new Conta("Santander", "Cartão Santander", TipoConta.CARTAO_CREDITO);
    private final Cartao cartao4948 = new Cartao(contaCartao, "4948");
    private final Cartao cartao1870 = new Cartao(contaCartao, "1870");

    private final Pessoa alison = new Pessoa("Alison", TipoPessoa.PESSOA);
    private final Pessoa gabriella = new Pessoa("Gabriella", TipoPessoa.PESSOA);
    private final Pessoa nelson = new Pessoa("Nelson", TipoPessoa.PESSOA);

    @Test
    @DisplayName("Cada atributo vem da regra mais forte que o define (Wellhub da Gabriella)")
    void atributosIndependentes() {
        Regra wellhub = regra(TipoChave.DESCRICAO_CONTEM, "wellhub");
        wellhub.setDescricao("Wellhub");
        wellhub.setCategoria("Academia");

        Regra wellhubGabriella = regra(TipoChave.DESCRICAO_CONTEM, "Wellhub Gabriella");
        wellhubGabriella.setPessoa(gabriella);

        Transacao t = compra("Wellhub Gabriella De Jesu", cartao4948);

        ResultadoLegenda resultado = motor.avaliar(t, List.of(wellhub, wellhubGabriella));

        assertThat(resultado.regraDescricao()).isSameAs(wellhub);
        assertThat(resultado.regraCategoria()).isSameAs(wellhub);
        assertThat(resultado.regraPessoa()).isSameAs(wellhubGabriella);
    }

    @Test
    @DisplayName("Descrição igual vence descrição que contém")
    void igualVenceContem() {
        Regra contem = regra(TipoChave.DESCRICAO_CONTEM, "GEORGINA");
        contem.setDescricao("Outra coisa");

        Regra igual = regra(TipoChave.DESCRICAO_IGUAL, "GEORGINA APARECIDA DE");
        igual.setDescricao("Salão");

        ResultadoLegenda resultado = motor.avaliar(compra("GEORGINA APARECIDA DE", cartao4948),
                List.of(contem, igual));

        assertThat(resultado.regraDescricao()).isSameAs(igual);
    }

    @Test
    @DisplayName("CNPJ vence descrição")
    void cnpjVenceDescricao() {
        Regra porDescricao = regra(TipoChave.DESCRICAO_CONTEM, "SPOTIFY");
        porDescricao.setDescricao("Spotify");

        Regra porCnpj = regra(TipoChave.CNPJ, "17.687.734/0001-93");
        porCnpj.setDescricao("Spotify Família");

        Transacao t = compra("EBN *SPOTIFY", cartao4948);
        t.setCnpj("17687734000193");

        assertThat(motor.avaliar(t, List.of(porDescricao, porCnpj)).regraDescricao()).isSameAs(porCnpj);
    }

    @Test
    @DisplayName("Regra com filtro de cartão só vale para aquele cartão, e vence a sem filtro")
    void filtroDeCartao() {
        Regra uberGeral = regra(TipoChave.DESCRICAO_CONTEM, "UBER");
        uberGeral.setDescricao("Uber");

        Regra uberDoPai = regra(TipoChave.DESCRICAO_CONTEM, "UBER");
        uberDoPai.setCartaoFinal("1870");
        uberDoPai.setPessoa(nelson);

        List<Regra> regras = List.of(uberGeral, uberDoPai);

        ResultadoLegenda no4948 = motor.avaliar(compra("DL*UberRides", cartao4948), regras);
        assertThat(no4948.regraPessoa()).isNull();
        assertThat(no4948.regraDescricao()).isSameAs(uberGeral);

        ResultadoLegenda no1870 = motor.avaliar(compra("DL*UberRides", cartao1870), regras);
        assertThat(no1870.regraPessoa()).isSameAs(uberDoPai);
    }

    @Test
    @DisplayName("Regra temporária vence a permanente dentro da vigência, e não vale fora dela")
    void regraTemporaria() {
        Regra permanente = regra(TipoChave.DESCRICAO_CONTEM, "UBER");
        permanente.setPessoa(alison);

        Regra temporaria = regra(TipoChave.DESCRICAO_CONTEM, "UBER");
        temporaria.setPessoa(nelson);
        temporaria.definirVigencia(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        List<Regra> regras = List.of(permanente, temporaria);

        Transacao emNovembro = compra("DL*UberRides", cartao4948, "2026-11-10T15:00:00Z");
        assertThat(motor.avaliar(emNovembro, regras).regraPessoa()).isSameAs(temporaria);

        Transacao emDezembro = compra("DL*UberRides", cartao4948, "2026-12-10T15:00:00Z");
        assertThat(motor.avaliar(emDezembro, regras).regraPessoa()).isSameAs(permanente);
    }

    @Test
    @DisplayName("Regra de MCC só define a categoria")
    void mccSoCategoria() {
        Regra mercado = regra(TipoChave.MCC, "5411");
        mercado.setCategoria("Mercado");
        mercado.setDescricao("Não deveria ser usada");
        mercado.setPessoa(alison);

        Transacao t = compra("AMIGAO BRASIL", cartao4948);
        t.setMcc(5411);

        ResultadoLegenda resultado = motor.avaliar(t, List.of(mercado));

        assertThat(resultado.regraCategoria()).isSameAs(mercado);
        assertThat(resultado.regraDescricao()).isNull();
        assertThat(resultado.regraPessoa()).isNull();
    }

    @Test
    @DisplayName("Regra desativada é ignorada")
    void regraDesativada() {
        Regra regra = regra(TipoChave.DESCRICAO_CONTEM, "UBER");
        regra.setDescricao("Uber");
        regra.setAtiva(false);

        assertThat(motor.avaliar(compra("DL*UberRides", cartao4948), List.of(regra)).regraPrincipal()).isNull();
    }

    @Test
    @DisplayName("A comparação ignora maiúsculas, acentos e espaços duplicados")
    void normalizacao() {
        Regra regra = regra(TipoChave.DESCRICAO_CONTEM, "  café   lima ");
        regra.setDescricao("Café");

        assertThat(motor.avaliar(compra("IFD*REST CAFE LIMA", cartao4948), List.of(regra)).regraDescricao())
                .isSameAs(regra);
    }

    @Test
    @DisplayName("Compra que nenhuma regra reconhece fica sem resultado")
    void semRegra() {
        Regra regra = regra(TipoChave.DESCRICAO_CONTEM, "SPOTIFY");
        regra.setDescricao("Spotify");

        assertThat(motor.avaliar(compra("VwConveniencia", cartao4948), List.of(regra)).regraPrincipal()).isNull();
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private Regra regra(TipoChave tipo, String valor) {
        return new Regra(tipo, valor);
    }

    private Transacao compra(String descricao, Cartao cartao) {
        return compra(descricao, cartao, "2026-11-01T15:00:00Z");
    }

    private Transacao compra(String descricao, Cartao cartao, String quando) {
        Transacao t = new Transacao("pluggy-" + descricao.hashCode(), contaCartao, Instant.parse(quando),
                new BigDecimal("10.00"), descricao, TipoMovimento.DEBITO, StatusTransacao.CONFIRMADA);
        t.setCartao(cartao);
        return t;
    }
}
