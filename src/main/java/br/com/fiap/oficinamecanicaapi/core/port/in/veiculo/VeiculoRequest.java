package br.com.fiap.oficinamecanicaapi.core.port.in.veiculo;

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
