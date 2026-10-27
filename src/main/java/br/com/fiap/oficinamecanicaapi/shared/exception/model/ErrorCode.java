package br.com.fiap.oficinamecanicaapi.shared.exception.model;

public enum ErrorCode {
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    CONFLICT(409);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
