package dev.alisonsantos.appgastos.importacao;

/**
 * As seções da planilha dentro de cada cartão.
 * Cada uma corresponde a uma pessoa (escolhida no upload).
 */
public enum SecaoPlanilha {

    /** As compras logo abaixo do título do cartão: as suas. */
    EU,

    /** "COMPRAS MÃE" */
    MAE,

    /** "COMPRAS PAI" */
    PAI,

    /** "COMPRAS MINHA VIDA" */
    MINHA_VIDA,

    /** Outras seções (ex.: "OUTRAS COMPRAS"): não indicam dono. */
    OUTRA
}
