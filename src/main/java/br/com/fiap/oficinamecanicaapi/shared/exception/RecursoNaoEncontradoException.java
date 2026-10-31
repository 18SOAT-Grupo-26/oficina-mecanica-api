package br.com.fiap.oficinamecanicaapi.shared.exception;

/**
 * O recurso pedido não existe (ou não está ativo).
 */
public final class RecursoNaoEncontradoException extends DominioException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
