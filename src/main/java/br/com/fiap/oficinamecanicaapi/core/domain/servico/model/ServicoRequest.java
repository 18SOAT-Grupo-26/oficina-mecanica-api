package br.com.fiap.oficinamecanicaapi.core.domain.servico.model;

import java.math.BigDecimal;

public record ServicoRequest(
        String nome,
        String descricao,
        BigDecimal preco
) {
}
