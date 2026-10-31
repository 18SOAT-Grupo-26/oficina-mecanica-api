package br.com.fiap.oficinamecanicaapi.core.port.in.servico;

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
