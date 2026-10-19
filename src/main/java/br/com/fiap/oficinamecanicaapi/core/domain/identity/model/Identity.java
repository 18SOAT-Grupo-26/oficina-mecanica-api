package br.com.fiap.oficinamecanicaapi.core.domain.identity.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record Identity(
        @NotBlank String fullName,
        String givenName,
        @NotBlank String cpf,
        String rg,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull @Valid Contact contact,
        @NotNull @Valid Address address
) {
}
