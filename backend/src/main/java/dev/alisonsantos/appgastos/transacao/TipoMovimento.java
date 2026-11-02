package dev.alisonsantos.appgastos.transacao;

/**
 * Direção do dinheiro, como a Pluggy informa no campo "type".
 *
 * DEBITO: saída (compra, pagamento, Pix enviado).
 * CREDITO: entrada (estorno, Pix recebido, pagamento de fatura no cartão).
 */
public enum TipoMovimento {
    DEBITO,
    CREDITO
}
