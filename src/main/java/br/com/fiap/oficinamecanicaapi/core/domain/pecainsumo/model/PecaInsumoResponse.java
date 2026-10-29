package br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PecaInsumoResponse(
        Long id,
        String nome,
        String descricao,
        Integer quantidade,
        Integer reservado,
        BigDecimal preco,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
