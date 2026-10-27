package br.com.fiap.oficinamecanicaapi.shared.handler;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class HandlerException {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorTemplate> handleApplicationException(ApplicationException e) {

        log.error("ApplicationException: {}", e.getMessage(), e);

        HttpStatus status = HttpStatus.resolve(e.getErrorCode());

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ErrorTemplate errorBody = ErrorTemplate.builder()
                .errorCode(e.getErrorCode())
                .message(e.getMessage())
                .timestamp(e.getTimestamp())
                .traceId(e.getTraceId())
                .build();

        log.info("Returning error body: {}", errorBody);

        return ResponseEntity.status(status).body(errorBody);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorTemplate> handleUnreadableBody(HttpMessageNotReadableException e) {

        ErrorTemplate errorBody = ErrorTemplate.builder()
                .errorCode(422)
                .message("Requisição inválida: verifique o formato e o tipo dos campos")
                .timestamp(java.time.LocalDateTime.now())
                .traceId(java.util.UUID.randomUUID().toString())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorBody);
    }

    /** Bean Validation (@Valid) — informa só o nome do campo, nunca o valor recebido. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorTemplate> handleInvalidArgument(MethodArgumentNotValidException e) {
        String campos = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
        return buildError(HttpStatus.BAD_REQUEST, "Campos inválidos: " + campos);
    }

    /** Header obrigatório ausente (ex.: Authorization no /auth/validate); sem este handler cairia no 500. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorTemplate> handleMissingHeader(MissingRequestHeaderException e) {
        return buildError(HttpStatus.BAD_REQUEST, "Header obrigatório ausente: " + e.getHeaderName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorTemplate> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return buildError(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + e.getName());
    }

    /** @PreAuthorize negado; sem este handler o genérico abaixo devolveria 500. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorTemplate> handleAccessDenied(AccessDeniedException e) {
        return buildError(HttpStatus.FORBIDDEN, "Acesso negado");
    }

    /** Banco fora do ar ou lento demais: 503 explícito, sem detalhes da falha (RNF04, ADR 003). */
    @ExceptionHandler({DataAccessResourceFailureException.class,
            CannotCreateTransactionException.class,
            QueryTimeoutException.class})
    public ResponseEntity<ErrorTemplate> handleDatabaseUnavailable(Exception e) {
        log.error("Database unavailable", e);
        return buildError(HttpStatus.SERVICE_UNAVAILABLE,
                "Serviço temporariamente indisponível. Tente novamente.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorTemplate> handleGenericException(Exception e) {

        log.error("Unexpected error", e);

        ErrorTemplate errorBody = ErrorTemplate.builder()
                .errorCode(500)
                .message("Erro interno inesperado")
                .timestamp(java.time.LocalDateTime.now())
                .traceId(java.util.UUID.randomUUID().toString())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
    }

    private static ResponseEntity<ErrorTemplate> buildError(HttpStatus status, String message) {
        ErrorTemplate errorBody = ErrorTemplate.builder()
                .errorCode(status.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .traceId(UUID.randomUUID().toString())
                .build();

        return ResponseEntity.status(status).body(errorBody);
    }
}