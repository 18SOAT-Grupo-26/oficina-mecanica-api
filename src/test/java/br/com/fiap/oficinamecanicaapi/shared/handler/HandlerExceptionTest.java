package br.com.fiap.oficinamecanicaapi.shared.handler;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandlerExceptionTest {

    private final HandlerException handler = new HandlerException();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @BeforeEach
    void registrarMetricas() {
        // os contadores do handler usam o registry global (Metrics); este registry só observa
        Metrics.addRegistry(registry);
    }

    @AfterEach
    void limpar() {
        Metrics.removeRegistry(registry);
        RequestContextHolder.resetRequestAttributes();
    }

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

    @ParameterizedTest(name = "código {0} → HTTP {1}")
    @CsvSource({"400,400", "401,401", "404,404", "409,409", "422,422", "500,500", "503,503", "999,500"})
    @DisplayName("ApplicationException deve virar o status do errorCode; código desconhecido vira 500")
    void applicationException(int codigo, int statusEsperado) {
        var ex = new ApplicationException(codigo, "mensagem");

        ResponseEntity<ErrorTemplate> response = handler.handleApplicationException(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(statusEsperado);
        assertThat(response.getBody().getErrorCode()).isEqualTo(codigo);
        assertThat(response.getBody().getMessage()).isEqualTo("mensagem");
        assertThat(response.getBody().getTraceId()).isEqualTo(ex.getTraceId());
    }

    @Test
    @DisplayName("Corpo ilegível deve responder 422 sem repetir o erro do parser")
    void corpoIlegivel() {
        var ex = mock(HttpMessageNotReadableException.class);

        var response = handler.handleUnreadableBody(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Requisição inválida: verifique o formato e o tipo dos campos");
    }

    @Test
    @DisplayName("@Valid deve listar os campos ordenados e sem repetição, nunca os valores")
    void camposInvalidos() {
        var resultado = new BeanPropertyBindingResult(new Object(), "request");
        resultado.addError(new FieldError("request", "senha", "segredo123", false, null, null, "obrigatório"));
        resultado.addError(new FieldError("request", "email", "x", false, null, null, "inválido"));
        resultado.addError(new FieldError("request", "senha", "segredo123", false, null, null, "curta"));
        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(resultado);

        var response = handler.handleInvalidArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Campos inválidos: email, senha");
        assertThat(response.getBody().getMessage()).doesNotContain("segredo123");
    }

    @Test
    @DisplayName("Header ausente deve responder 400 com o nome do header")
    void headerAusente() {
        var ex = mock(MissingRequestHeaderException.class);
        when(ex.getHeaderName()).thenReturn("Authorization");

        var response = handler.handleMissingHeader(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Header obrigatório ausente: Authorization");
    }

    @Test
    @DisplayName("Tipo de parâmetro errado deve responder 400 com o nome do parâmetro")
    void tipoInvalido() {
        var ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");

        var response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Parâmetro inválido: id");
    }

    @Test
    @DisplayName("Acesso negado deve responder 403 e contar oficina.seguranca.acesso.negado")
    void acessoNegado() {
        var response = handler.handleAccessDenied(new AccessDeniedException("Access Denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getMessage()).isEqualTo("Acesso negado");
        assertThat(registry.get("oficina.seguranca.acesso.negado").tag("status", "403").counter().count())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("Banco indisponível deve contar oficina.banco.indisponivel")
    void bancoIndisponivelConta() {
        handler.handleDatabaseUnavailable(new org.springframework.dao.QueryTimeoutException("timeout"));

        assertThat(registry.get("oficina.banco.indisponivel").counter().count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Erro genérico deve responder 500 sem expor a mensagem original, também dentro de uma requisição")
    void erroGenerico() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest("GET", "/api/v1/clientes/9")));

        var response = handler.handleGenericException(new IllegalStateException("NullPointer em Foo.java:42"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getErrorCode()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Erro interno inesperado");
        assertThat(response.getBody().getTraceId()).isNotBlank();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}
