package dev.alisonsantos.appgastos.importacao;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do leitor da planilha.
 *
 * Em vez de usar um arquivo de verdade (que teria dados pessoais), o teste
 * MONTA uma planilha pequena em memória, com o mesmo formato da real.
 */
@DisplayName("Importador: leitura da planilha")
class LeitorPlanilhaTest {

    private final LeitorPlanilha leitor = new LeitorPlanilha();

    @Test
    @DisplayName("Lê as compras de cada seção, achando as colunas pelo nome do cabeçalho")
    void leSecoesEColunas() throws IOException {
        byte[] arquivo = planilha(aba -> {
            linha(aba, 0, "CARTÃO SANTANDER");
            linha(aba, 1, "Data da Compra", "Cartão", "Categoria", "Descrição Fornecedor", "Preço Total");
            linha(aba, 2, null, "4948", "Spotify", "EBN*SPOTIFY", "-");
            linha(aba, 3, "COMPRAS MÃE");
            linha(aba, 4, null, "3145", "Compras Mercado Mãe", "Atacadista Bem Bom", "-");
            linha(aba, 5, "JUROS");
            linha(aba, 6, null, "1603", "Juros boleto", "JIM.COM", "-");
            linha(aba, 7, "COMPRAS PAI");
            linha(aba, 8, null, "3145", "-", "Linha sem nome", "-");
            linha(aba, 9, "GASTOS MENSAIS ( FIXOS )");
            linha(aba, 10, null, "-", "Conta de luz", "Copel", "-");
        });

        List<LinhaPlanilha> linhas = leitor.ler(new ByteArrayInputStream(arquivo));

        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).secao()).isEqualTo(SecaoPlanilha.EU);
        assertThat(linhas.get(0).descricaoAmigavel()).isEqualTo("Spotify");
        assertThat(linhas.get(0).descricaoFornecedor()).isEqualTo("EBN*SPOTIFY");
        assertThat(linhas.get(1).secao()).isEqualTo(SecaoPlanilha.MAE);
        assertThat(linhas.get(1).descricaoFornecedor()).isEqualTo("Atacadista Bem Bom");
    }

    @Test
    @DisplayName("Funciona com o formato antigo, em que as colunas estavam em outra ordem")
    void formatoAntigo() throws IOException {
        byte[] arquivo = planilha(aba -> {
            linha(aba, 0, "CARTÃO DIGIO");
            linha(aba, 1, "Data da Compra", "Categoria", "Descrição Fornecedor", "Preço Total");
            linha(aba, 2, null, "Netflix", "NETFLIX.COM", "-");
        });

        List<LinhaPlanilha> linhas = leitor.ler(new ByteArrayInputStream(arquivo));

        assertThat(linhas).singleElement().satisfies(l -> {
            assertThat(l.descricaoAmigavel()).isEqualTo("Netflix");
            assertThat(l.descricaoFornecedor()).isEqualTo("NETFLIX.COM");
        });
    }

    // ------------------------------------------------------------------
    // Apoio: monta uma planilha .xlsx em memória
    // ------------------------------------------------------------------

    private interface MontagemAba {
        void montar(Sheet aba);
    }

    private byte[] planilha(MontagemAba montagem) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            montagem.montar(workbook.createSheet("NOVEMBRO-2026"));
            workbook.write(saida);
            return saida.toByteArray();
        }
    }

    private void linha(Sheet aba, int numero, String... valores) {
        Row linha = aba.createRow(numero);
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] != null) {
                linha.createCell(i).setCellValue(valores[i]);
            }
        }
    }
}
