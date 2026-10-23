package br.com.fiap.oficinamecanicaapi.core.domain.servico.model;

import java.math.BigDecimal;
import java.util.List;

public record ServicoRequest(
        String nome,
        String descricao,
        BigDecimal preco,
        List<Long> pecasInsumoIds
) {
}
