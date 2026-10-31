package br.com.fiap.oficinamecanicaapi.shared.exception;

/**
 * Entrada inválida ou incompleta (campo obrigatório, formato, faixa de valores).
 */
public final class DadoInvalidoException extends DominioException {

    public DadoInvalidoException(String message) {
        super(message);
    }
}
