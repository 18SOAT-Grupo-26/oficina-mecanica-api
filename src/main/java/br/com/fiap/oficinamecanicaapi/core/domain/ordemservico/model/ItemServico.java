package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import lombok.Getter;

/**
 * Serviço incluído na OS, com referência ao serviço do catálogo.
 */
@Getter
public class ItemServico extends ItemOrdemServico {

    private final Long idServico;

    private ItemServico(Long id, Long idServico, String descricao, ValorMonetario precoUnitario, int quantidade) {
        if (idServico == null) {
            throw new ApplicationException(400, "O serviço do item é obrigatório");
        }
        super(id, descricao, precoUnitario, quantidade);
        this.idServico = idServico;
    }

    /**
     * Cria um item novo, ainda não persistido.
     */
    public static ItemServico novo(Long idServico, String descricao, ValorMonetario precoUnitario, int quantidade) {
        return new ItemServico(null, idServico, descricao, precoUnitario, quantidade);
    }

    /**
     * Reconstrói um item já persistido (uso exclusivo dos adapters de saída).
     */
    public static ItemServico reconstituir(Long id, Long idServico, String descricao,
                                           ValorMonetario precoUnitario, int quantidade) {
        return new ItemServico(id, idServico, descricao, precoUnitario, quantidade);
    }
}
