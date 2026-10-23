package br.com.fiap.oficinamecanicaapi.core.domain.servico.model;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ServicoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        List<PecaInsumoResponse> pecasInsumo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}
