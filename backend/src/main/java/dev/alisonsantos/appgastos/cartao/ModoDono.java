package dev.alisonsantos.appgastos.cartao;

/**
 * Como o dono das compras de um cartão é definido.
 *
 * FIXO: toda compra vai para o dono padrão do cartão.
 * PERGUNTAR: cartão compartilhado; cada compra cai na caixa de entrada
 * para escolher de quem é (ex.: cartão usado no Uber de duas pessoas).
 */
public enum ModoDono {
    FIXO,
    PERGUNTAR
}
