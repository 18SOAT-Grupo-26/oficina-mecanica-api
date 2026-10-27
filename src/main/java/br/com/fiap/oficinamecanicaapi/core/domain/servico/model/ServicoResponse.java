package br.com.fiap.oficinamecanicaapi.core.domain.servico.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ServicoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
