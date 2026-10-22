package br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VeiculoRequest(
        @NotBlank String placa,
        @NotBlank String marca,
        @NotBlank String modelo,
        @NotNull Integer ano,
        @NotNull Long clienteId
) {
}
