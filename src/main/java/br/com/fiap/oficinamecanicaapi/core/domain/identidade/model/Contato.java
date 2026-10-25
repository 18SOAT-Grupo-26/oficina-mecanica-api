package br.com.fiap.oficinamecanicaapi.core.domain.identidade.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record Contato(
        @NotNull @Valid String telefone,
        @NotNull @Valid String email
) {
}
