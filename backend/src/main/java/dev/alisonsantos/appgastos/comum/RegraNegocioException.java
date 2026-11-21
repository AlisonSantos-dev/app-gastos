package dev.alisonsantos.appgastos.comum;

/**
 * Lançada quando o pedido é válido no formato, mas quebra uma regra do app
 * (ex.: já existe uma pessoa com esse nome).
 * Vira HTTP 422 no ApiExceptionHandler.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
