package br.com.fiap.oficinamecanicaapi.shared.exception;

/**
 * A operação conflita com o estado atual: duplicidade, transição de status não permitida, estoque insuficiente.
 */
public final class ConflitoException extends DominioException {

    public ConflitoException(String message) {
        super(message);
    }
}
