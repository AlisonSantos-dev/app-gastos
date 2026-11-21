package dev.alisonsantos.appgastos.comum;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Funções de texto usadas em vários lugares do app.
 */
public final class Texto {

    private Texto() {
        // Só funções estáticas
    }

    /**
     * Deixa o texto pronto para comparar: maiúsculas, sem acentos e sem
     * espaços duplicados. Assim "Café  Lima" e "CAFE LIMA" ficam iguais.
     * Null vira texto vazio, para não precisar testar null toda hora.
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /** Só os números do texto. Ex.: "06.057.223/0001-71" vira "06057223000171". */
    public static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }
}
