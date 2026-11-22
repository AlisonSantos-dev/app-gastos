package dev.alisonsantos.appgastos.importacao;

import dev.alisonsantos.appgastos.comum.Texto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê a planilha de gastos (.xlsx) com o Apache POI.
 *
 * A planilha mudou de formato ao longo dos anos (colunas trocaram de
 * lugar), então o leitor NÃO usa posições fixas: ele acha a linha de
 * cabeçalho ("Data da Compra") e descobre em qual coluna está cada
 * informação pelo nome.
 *
 * Ele percorre cada aba como uma "máquina de estados":
 *   - "CARTÃO ..."        → começa um bloco de cartão (seção: EU)
 *   - "COMPRAS MÃE/PAI/MINHA VIDA" → muda a seção (de quem é a compra)
 *   - "JUROS"             → ignora até o próximo título
 *   - "GASTOS ..."        → sai do bloco de cartão (contas fixas, não interessam aqui)
 *   - "Data da Compra"    → é o cabeçalho: mapeia as colunas
 *   - qualquer outra linha dentro de um cartão → é uma compra
 */
@Component
public class LeitorPlanilha {

    private static final String COLUNA_AMIGAVEL = "CATEGORIA";
    private static final String COLUNA_FORNECEDOR = "DESCRICAO FORNECEDOR";

    private final DataFormatter formatador = new DataFormatter();

    public List<LinhaPlanilha> ler(InputStream arquivo) throws IOException {
        List<LinhaPlanilha> linhas = new ArrayList<>();
        try (Workbook planilha = WorkbookFactory.create(arquivo)) {
            for (Sheet aba : planilha) {
                lerAba(aba, linhas);
            }
        }
        return linhas;
    }

    private void lerAba(Sheet aba, List<LinhaPlanilha> linhas) {
        boolean dentroDeCartao = false;
        SecaoPlanilha secao = SecaoPlanilha.EU;
        boolean ignorando = false;
        Map<String, Integer> colunas = null;

        for (Row linha : aba) {
            String primeira = Texto.normalizar(texto(linha.getCell(0)));

            // ---------------- Títulos ----------------
            if (primeira.startsWith("CARTAO")) {
                dentroDeCartao = true;
                secao = SecaoPlanilha.EU;
                ignorando = false;
                colunas = null;
                continue;
            }
            if (primeira.startsWith("GASTOS")) {
                dentroDeCartao = false;
                continue;
            }
            switch (primeira) {
                case "COMPRAS MAE" -> { secao = SecaoPlanilha.MAE; ignorando = false; continue; }
                case "COMPRAS PAI" -> { secao = SecaoPlanilha.PAI; ignorando = false; continue; }
                case "COMPRAS MINHA VIDA" -> { secao = SecaoPlanilha.MINHA_VIDA; ignorando = false; continue; }
                case "OUTRAS COMPRAS" -> { secao = SecaoPlanilha.OUTRA; ignorando = false; continue; }
                case "JUROS" -> { ignorando = true; continue; }
                case "DATA DA COMPRA" -> { colunas = mapearColunas(linha); continue; }
                default -> { }
            }

            // ---------------- Compras ----------------
            if (!dentroDeCartao || ignorando || colunas == null) {
                continue;
            }
            Integer colAmigavel = colunas.get(COLUNA_AMIGAVEL);
            Integer colFornecedor = colunas.get(COLUNA_FORNECEDOR);
            if (colAmigavel == null || colFornecedor == null) {
                continue;
            }

            String amigavel = limpo(texto(linha.getCell(colAmigavel)));
            String fornecedor = limpo(texto(linha.getCell(colFornecedor)));
            if (amigavel == null || fornecedor == null) {
                continue;
            }

            linhas.add(new LinhaPlanilha(aba.getSheetName(), secao, data(linha.getCell(0)), amigavel, fornecedor));
        }
    }

    /** "Nome da coluna normalizado" → índice da coluna. */
    private Map<String, Integer> mapearColunas(Row cabecalho) {
        Map<String, Integer> colunas = new HashMap<>();
        for (Cell celula : cabecalho) {
            String nome = Texto.normalizar(texto(celula));
            if (!nome.isEmpty()) {
                colunas.putIfAbsent(nome, celula.getColumnIndex());
            }
        }
        return colunas;
    }

    /** O texto da célula, do jeito que aparece no Excel. */
    private String texto(Cell celula) {
        if (celula == null) {
            return null;
        }
        CellType tipo = celula.getCellType() == CellType.FORMULA
                ? celula.getCachedFormulaResultType()
                : celula.getCellType();
        return switch (tipo) {
            case STRING -> celula.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(celula) ? null : formatador.formatCellValue(celula);
            default -> null;
        };
    }

    /** A data da célula, se for uma data. */
    private LocalDate data(Cell celula) {
        if (celula == null) {
            return null;
        }
        CellType tipo = celula.getCellType() == CellType.FORMULA
                ? celula.getCachedFormulaResultType()
                : celula.getCellType();
        if (tipo == CellType.NUMERIC && DateUtil.isCellDateFormatted(celula)) {
            return celula.getLocalDateTimeCellValue().toLocalDate();
        }
        return null;
    }

    /** Texto vazio ou "-" (usado na planilha como "nada") vira null. */
    private String limpo(String texto) {
        if (texto == null) {
            return null;
        }
        String semEspacos = texto.trim();
        return semEspacos.isEmpty() || semEspacos.equals("-") ? null : semEspacos;
    }
}
