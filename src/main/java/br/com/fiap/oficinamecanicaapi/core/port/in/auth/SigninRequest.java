package br.com.fiap.oficinamecanicaapi.core.port.in.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SigninRequest(
        @NotBlank String identificador,
        @NotBlank String senha
) {
}
