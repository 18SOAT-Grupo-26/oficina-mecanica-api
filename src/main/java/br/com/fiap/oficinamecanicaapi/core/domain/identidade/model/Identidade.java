package br.com.fiap.oficinamecanicaapi.core.domain.identidade.model;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Endereco;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record Identidade(
        @NotBlank String nomeCompleto,
        String primeiroNome,
        @NotBlank String documento,
        String rg,
        @NotNull @Past LocalDate dataNascimento,
        @NotNull @Valid Contato contato,
        @NotNull @Valid Endereco endereco
) {
}
