package br.com.fiap.oficinamecanicaapi.shared.exception;

/**
 * Erro de regra ou de entrada do domínio, sem nada de HTTP: quem decide o status da resposta é o adapter web
 * ({@code HandlerException}). É selada para o {@code switch} do handler ser exaustivo: uma exceção nova sem status
 * mapeado não compila.
 */
public abstract sealed class DominioException extends RuntimeException
        permits DadoInvalidoException, RecursoNaoEncontradoException, ConflitoException, NaoAutenticadoException,
        LimiteExcedidoException {

    protected DominioException(String message) {
        super(message);
    }
}
