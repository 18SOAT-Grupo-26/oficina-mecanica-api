package br.com.fiap.oficinamecanicaapi.core.domain.auth.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SigninRequest(
        @NotBlank String identifier,
        @NotBlank String password
) {
}
