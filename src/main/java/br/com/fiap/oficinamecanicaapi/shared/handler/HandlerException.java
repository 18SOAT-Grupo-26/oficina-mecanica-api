package br.com.fiap.oficinamecanicaapi.shared.handler;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorTemplate> handleGenericException(Exception e) {

        log.error("Unexpected error", e);

        ErrorTemplate errorBody = ErrorTemplate.builder()
                .errorCode(500)
                .message("Unexpected internal error")
                .timestamp(java.time.LocalDateTime.now())
                .traceId(java.util.UUID.randomUUID().toString())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
    }
}