package br.com.fiap.oficinamecanicaapi.core.port.in.servico;

import java.math.BigDecimal;

public record ServicoRequest(
        String nome,
        String descricao,
        BigDecimal preco
) {
}
