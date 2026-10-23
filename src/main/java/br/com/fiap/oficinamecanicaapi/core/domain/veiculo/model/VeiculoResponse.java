package br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model;

import java.time.OffsetDateTime;

public record VeiculoResponse(
        Long id,
        String placa,
        String marca,
        String modelo,
        Integer ano,
        Long clienteId,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
