package br.com.fiap.oficinamecanicaapi.core.domain.cliente.model;

import java.time.OffsetDateTime;

public record ClienteResponse(
        Long id,
        String nome,
        String documento,
        TipoDocumento tipoDocumento,
        String email,
        String telefone,
        Endereco endereco,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
