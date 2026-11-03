package br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo;

import java.math.BigDecimal;

public record PecaInsumoRequest(
        String nome,
        String descricao,
        Integer quantidade,
        Integer reservado,
        BigDecimal preco
) {
}
