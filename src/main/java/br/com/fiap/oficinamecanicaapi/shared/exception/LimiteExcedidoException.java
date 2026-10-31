package br.com.fiap.oficinamecanicaapi.shared.exception;

/**
 * Limite de uso atingido (ex.: tentativas de login).
 */
public final class LimiteExcedidoException extends DominioException {

    public LimiteExcedidoException(String message) {
        super(message);
    }
}
