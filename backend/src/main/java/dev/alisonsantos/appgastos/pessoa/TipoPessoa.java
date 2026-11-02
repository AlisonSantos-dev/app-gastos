package dev.alisonsantos.appgastos.pessoa;

/**
 * Indica se o destino de uma compra é uma pessoa ou um grupo.
 *
 * PESSOA: alguém da família ou um terceiro (com ou sem login no app).
 * GRUPO: um destino compartilhado, como "Contas da casa".
 */
public enum TipoPessoa {
    PESSOA,
    GRUPO
}
