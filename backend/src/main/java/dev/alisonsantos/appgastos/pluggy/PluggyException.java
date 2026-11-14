package dev.alisonsantos.appgastos.pluggy;

/**
 * Erro ao conversar com a Pluggy.
 *
 * Traz uma mensagem em português, que explica o que fazer,
 * em vez do erro técnico da biblioteca HTTP.
 */
public class PluggyException extends RuntimeException {

    public PluggyException(String mensagem) {
        super(mensagem);
    }

    public PluggyException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
