package dev.alisonsantos.appgastos.legenda;

/**
 * O que a legenda decidiu para uma transação.
 *
 * Cada atributo é decidido de forma independente: a regra que dá o nome
 * pode ser uma, e a que define o dono pode ser outra. Por exemplo:
 *   - "contém WELLHUB"           → descrição "Wellhub", categoria "Academia"
 *   - "contém WELLHUB GABRIELLA" → pessoa Gabriella
 *
 * Qualquer um pode ser null (nenhuma regra definiu aquele atributo).
 */
public record ResultadoLegenda(
        Regra regraDescricao,
        Regra regraCategoria,
        Regra regraPessoa
) {

    /** Nenhuma regra bateu. */
    public static ResultadoLegenda vazio() {
        return new ResultadoLegenda(null, null, null);
    }

    /** A regra "principal", a que fica registrada na transação. */
    public Regra regraPrincipal() {
        if (regraDescricao != null) {
            return regraDescricao;
        }
        return regraPessoa != null ? regraPessoa : regraCategoria;
    }
}
