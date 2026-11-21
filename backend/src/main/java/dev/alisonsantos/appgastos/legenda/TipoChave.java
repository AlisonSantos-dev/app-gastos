package dev.alisonsantos.appgastos.legenda;

import dev.alisonsantos.appgastos.comum.Texto;

/**
 * Por qual característica a regra reconhece a compra.
 *
 * O "nivel" define a ordem da cascata: quanto menor, mais específica
 * e mais forte a regra. Um CNPJ identifica a loja com certeza; um trecho
 * de descrição pode bater em várias lojas diferentes.
 */
public enum TipoChave {

    /** O CNPJ do estabelecimento (só números). O mais preciso. */
    CNPJ(1),

    /** O nome do estabelecimento que a Pluggy identificou. */
    MERCHANT(2),

    /** A descrição do banco inteira, exatamente igual. */
    DESCRICAO_IGUAL(3),

    /** Um trecho da descrição do banco. */
    DESCRICAO_CONTEM(4),

    /** O código do tipo de estabelecimento (ex.: 5411 = supermercado). Só define categoria. */
    MCC(5);

    private final int nivel;

    TipoChave(int nivel) {
        this.nivel = nivel;
    }

    public int getNivel() {
        return nivel;
    }

    /**
     * Normaliza o valor da chave do jeito certo para cada tipo:
     * CNPJ e MCC ficam só com números; os textos ficam em maiúsculas e sem acento.
     */
    public String normalizar(String valor) {
        return switch (this) {
            case CNPJ, MCC -> Texto.somenteDigitos(valor);
            case MERCHANT, DESCRICAO_IGUAL, DESCRICAO_CONTEM -> Texto.normalizar(valor);
        };
    }
}
