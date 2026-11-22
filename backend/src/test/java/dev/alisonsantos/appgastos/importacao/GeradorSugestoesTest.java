package dev.alisonsantos.appgastos.importacao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do gerador de sugestões, com casos tirados da planilha real.
 */
@DisplayName("Importador: sugestões de regras")
class GeradorSugestoesTest {

    private final GeradorSugestoes gerador = new GeradorSugestoes();

    @Test
    @DisplayName("Descrição usada em várias compras, sempre com o mesmo nome, vira sugestão")
    void sugestaoSimples() {
        List<LinhaPlanilha> linhas = List.of(
                linha("OUTUBRO-2026", SecaoPlanilha.MAE, "2026-10-05", "Compras Mercado Mãe", "Atacadista Bem Bom"),
                linha("OUTUBRO-2026", SecaoPlanilha.MAE, "2026-10-12", "Compras Mercado Mãe", "ATACADISTA  BEM BOM"),
                linha("NOVEMBRO-2026", SecaoPlanilha.MAE, "2026-11-04", "Compras mercado mãe", "Atacadista Bem Bom"));

        List<SugestaoCalculada> sugestoes = gerador.gerar(linhas,
                List.of("ATACADISTA BEM BOM PAR", "ATACADISTA BEM BOM PAR", "DL*UBERRIDES"));

        assertThat(sugestoes).hasSize(1);
        SugestaoCalculada s = sugestoes.getFirst();
        assertThat(s.valorChave()).isEqualTo("ATACADISTA BEM BOM");
        assertThat(s.descricao()).isEqualTo("Compras Mercado Mãe");
        assertThat(s.compras()).isEqualTo(3);
        assertThat(s.confiancaDescricao()).isEqualTo(1.0);
        assertThat(s.secao()).isEqualTo(SecaoPlanilha.MAE);
        assertThat(s.transacoesAfetadas()).isEqualTo(2);
        assertThat(s.ultimaCompra()).isEqualTo(LocalDate.of(2026, 11, 4));
    }

    @Test
    @DisplayName("As parcelas da mesma compra contam como UMA compra (a máquina de lavar não vira regra)")
    void parcelasContamUmaVez() {
        List<LinhaPlanilha> linhas = List.of(
                linha("MARCO-2024", SecaoPlanilha.EU, "2024-03-10", "Máquina de lavar", "Magazine Luiza"),
                linha("ABRIL-2024", SecaoPlanilha.EU, "2024-03-10", "Máquina de lavar", "Magazine Luiza"),
                linha("MAIO-2024", SecaoPlanilha.EU, "2024-03-10", "Máquina de lavar", "Magazine Luiza"));

        assertThat(gerador.gerar(linhas, List.of())).isEmpty();
    }

    @Test
    @DisplayName("Nomes muito diferentes para a mesma descrição (menos de 60% iguais) não viram sugestão")
    void nomesVariados() {
        List<LinhaPlanilha> linhas = List.of(
                linha("A", SecaoPlanilha.EU, "2026-01-01", "Presente aniversário", "MercadoLivre*Mercado"),
                linha("B", SecaoPlanilha.EU, "2026-02-01", "Tênis", "MercadoLivre*Mercado"),
                linha("C", SecaoPlanilha.EU, "2026-03-01", "Capa de celular", "MercadoLivre*Mercado"));

        assertThat(gerador.gerar(linhas, List.of())).isEmpty();
    }

    @Test
    @DisplayName("Sugestões que identificam mais transações do app vêm primeiro")
    void ordemPorImpacto() {
        List<LinhaPlanilha> linhas = List.of(
                linha("A", SecaoPlanilha.EU, "2026-01-05", "Netflix", "NETFLIX.COM"),
                linha("B", SecaoPlanilha.EU, "2026-02-05", "Netflix", "NETFLIX.COM"),
                linha("A", SecaoPlanilha.EU, "2026-01-08", "Spotify", "EBN*SPOTIFY"),
                linha("B", SecaoPlanilha.EU, "2026-02-08", "Spotify", "EBN*SPOTIFY"));

        List<SugestaoCalculada> sugestoes = gerador.gerar(linhas,
                List.of("EBN *SPOTIFY", "EBN*SPOTIFY", "EBN*SPOTIFY", "NETFLIX.COM"));

        assertThat(sugestoes).extracting(SugestaoCalculada::valorChave)
                .containsExactly("EBN*SPOTIFY", "NETFLIX.COM");
    }

    private LinhaPlanilha linha(String aba, SecaoPlanilha secao, String data, String amigavel, String fornecedor) {
        return new LinhaPlanilha(aba, secao, LocalDate.parse(data), amigavel, fornecedor);
    }
}
