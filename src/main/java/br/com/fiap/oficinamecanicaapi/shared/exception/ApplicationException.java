package br.com.fiap.oficinamecanicaapi.shared.exception;

import br.com.fiap.oficinamecanicaapi.shared.utils.GetLocalTime;
import lombok.Getter;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
public class ApplicationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    private final Integer errorCode;
    private final String message;
    private final OffsetDateTime timestamp;
    private final String traceId;

    public ApplicationException(int errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = GetLocalTime.getLocalTime();
        this.traceId = setTraceId();
    }

    private String setTraceId() {
        return UUID.randomUUID().toString();
    }

}
