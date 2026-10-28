package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import lombok.Getter;

/**
 * Peça ou insumo incluído na OS, com referência à peça do catálogo.
 */
@Getter
public class ItemPeca extends ItemOrdemServico {

    private final Long idPeca;

    private ItemPeca(Long id, Long idPeca, String descricao, ValorMonetario precoUnitario, int quantidade) {
        if (idPeca == null) {
            throw new ApplicationException(400, "A peça do item é obrigatória");
        }
        super(id, descricao, precoUnitario, quantidade);
        this.idPeca = idPeca;
    }

    /**
     * Cria um item novo, ainda não persistido.
     */
    public static ItemPeca novo(Long idPeca, String descricao, ValorMonetario precoUnitario, int quantidade) {
        return new ItemPeca(null, idPeca, descricao, precoUnitario, quantidade);
    }

    /**
     * Reconstrói um item já persistido (uso exclusivo dos adapters de saída).
     */
    public static ItemPeca reconstituir(Long id, Long idPeca, String descricao,
                                        ValorMonetario precoUnitario, int quantidade) {
        return new ItemPeca(id, idPeca, descricao, precoUnitario, quantidade);
    }
}
