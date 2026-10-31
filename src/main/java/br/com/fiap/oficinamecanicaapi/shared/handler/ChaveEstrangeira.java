package br.com.fiap.oficinamecanicaapi.shared.handler;

import org.hibernate.exception.ConstraintViolationException;

import java.util.Map;
import java.util.Optional;

/**
 * Traduz a violação de chave estrangeira do banco (ex.: excluir serviço ou peça que estão em uma ordem de serviço)
 * para a mensagem de negócio. Outras violações de integridade (unicidade, nulo) não são reconhecidas aqui.
 */
final class ChaveEstrangeira {

    /**
     * SQLSTATE padrão de violação de chave estrangeira (foreign_key_violation).
     */
    static final String SQLSTATE = "23503";
    static final String MENSAGEM_PADRAO = "O registro está em uso e não pode ser excluído";

    private static final Map<String, String> MENSAGENS = Map.of(
            "fk_item_servico_os_servico", "O serviço está em uma ordem de serviço e não pode ser excluído",
            "fk_item_peca_os_peca", "O insumo está em uma ordem de serviço e não pode ser excluído");

    private ChaveEstrangeira() {
    }

    /**
     * A mensagem de negócio, se o erro (ou uma das causas) for uma violação de chave estrangeira.
     */
    static Optional<String> mensagem(Throwable erro) {
        for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
            if (atual instanceof ConstraintViolationException violacao) {
                return SQLSTATE.equals(violacao.getSQLState())
                        ? Optional.of(MENSAGENS.getOrDefault(violacao.getConstraintName(), MENSAGEM_PADRAO))
                        : Optional.empty();
            }
        }
        return Optional.empty();
    }
}
