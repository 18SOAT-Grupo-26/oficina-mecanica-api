package br.com.fiap.oficinamecanicaapi.shared.handler;

import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class HandlerExceptionTest {

    private final HandlerException handler = new HandlerException();

    static Stream<Exception> falhasDeBanco() {
        return Stream.of(
                new DataAccessResourceFailureException("Connection refused"),
                new CannotCreateTransactionException("Could not open JPA EntityManager"),
                new QueryTimeoutException("canceling statement due to statement timeout"));
    }

    @ParameterizedTest
    @MethodSource("falhasDeBanco")
    @DisplayName("Deve responder 503 com mensagem explícita quando o banco estiver indisponível")
    void deveResponder503QuandoBancoIndisponivel(Exception falha) {
        ResponseEntity<ErrorTemplate> response = handler.handleDatabaseUnavailable(falha);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo(503);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Serviço temporariamente indisponível. Tente novamente.");
        assertThat(response.getBody().getTraceId()).isNotBlank();
    }

    @Test
    @DisplayName("Não deve expor a mensagem original da falha de banco")
    void naoDeveExporDetalhesDaFalha() {
        ResponseEntity<ErrorTemplate> response =
                handler.handleDatabaseUnavailable(new DataAccessResourceFailureException("jdbc:postgresql://db:5432"));

        assertThat(response.getBody().getMessage()).doesNotContain("jdbc", "postgresql");
    }
}
