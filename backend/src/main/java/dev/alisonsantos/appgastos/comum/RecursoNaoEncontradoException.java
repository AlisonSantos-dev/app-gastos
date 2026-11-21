package dev.alisonsantos.appgastos.comum;

/**
 * Lançada quando algo pedido pelo id não existe (ex.: pessoa 99).
 * Vira HTTP 404 no ApiExceptionHandler.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    /** Atalho: "Pessoa 99 não encontrada." */
    public static RecursoNaoEncontradoException de(String recurso, Long id) {
        return new RecursoNaoEncontradoException(recurso + " " + id + " não encontrado(a).");
    }
}
