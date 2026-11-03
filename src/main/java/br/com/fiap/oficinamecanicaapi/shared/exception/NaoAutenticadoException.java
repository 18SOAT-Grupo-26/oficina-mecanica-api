package br.com.fiap.oficinamecanicaapi.shared.exception;

/**
 * Credenciais ou token inválidos.
 */
public final class NaoAutenticadoException extends DominioException {

    public NaoAutenticadoException(String message) {
        super(message);
    }
}
