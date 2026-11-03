package br.com.fiap.oficinamecanicaapi.core.port.in.veiculo;

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
