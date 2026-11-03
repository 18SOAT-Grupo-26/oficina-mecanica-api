package br.com.fiap.oficinamecanicaapi.shared.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tratamento das violações de integridade do banco. Fica fora do HandlerExceptionTest para não disputar as mesmas
 * linhas com outras mudanças no handler.
 */
class HandlerExceptionIntegridadeTest {

    private final HandlerException handler = new HandlerException();

    @Test
    @DisplayName("Violação de chave estrangeira deve responder 409 com a mensagem de negócio, e não 500")
    void chaveEstrangeiraDeveResponder409() {
        var erro = ChaveEstrangeiraTest.integridade(ChaveEstrangeira.SQLSTATE, "fk_item_servico_os_servico");

        var response = handler.handleIntegridade(erro);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getErrorCode()).isEqualTo(409);
        assertThat(response.getBody().getMessage())
                .isEqualTo("O serviço está em uma ordem de serviço e não pode ser excluído");
        assertThat(response.getBody().getTraceId()).isNotBlank();
    }

    @Test
    @DisplayName("Outra violação de integridade deve continuar como 500 genérico")
    void outraViolacaoDeveResponder500() {
        var erro = ChaveEstrangeiraTest.integridade("23505", "uk_servico_nome");

        var response = handler.handleIntegridade(erro);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Erro interno inesperado");
    }
}
