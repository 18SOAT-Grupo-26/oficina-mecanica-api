package br.com.fiap.oficinamecanicaapi.core.port.in.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record AuthToken(
        @NotBlank String token,
        boolean trocaSenhaObrigatoria
) {
}
