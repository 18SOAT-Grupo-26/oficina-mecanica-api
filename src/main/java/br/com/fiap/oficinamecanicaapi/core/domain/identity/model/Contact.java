package br.com.fiap.oficinamecanicaapi.core.domain.identity.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record Contact(
        @NotNull @Valid String phoneNumber,
        @NotNull @Valid String email
) {
}
