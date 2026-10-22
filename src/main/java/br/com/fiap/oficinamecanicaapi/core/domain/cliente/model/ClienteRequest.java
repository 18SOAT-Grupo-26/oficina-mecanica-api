package br.com.fiap.oficinamecanicaapi.core.domain.cliente.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClienteRequest(
        @NotBlank String nome,
        @NotBlank String documento,
        @NotBlank @Email String email,
        @NotBlank String telefone,
        @Valid Endereco endereco
) {
}
