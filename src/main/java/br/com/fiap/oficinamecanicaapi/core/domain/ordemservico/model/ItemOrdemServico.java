package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import lombok.Getter;

/**
 * Base dos itens da OS. O preço unitário é o do catálogo no momento da inclusão e não muda
 * depois, mesmo que o catálogo mude.
 */
@Getter
public abstract class ItemOrdemServico {

    private final Long id;
    private final String descricao;
    private final ValorMonetario precoUnitario;
    private final int quantidade;

    protected ItemOrdemServico(Long id, String descricao, ValorMonetario precoUnitario, int quantidade) {
        if (descricao == null || descricao.isBlank()) {
            throw new ApplicationException(400, "A descrição do item é obrigatória");
        }
        if (precoUnitario == null) {
            throw new ApplicationException(400, "O preço unitário do item é obrigatório");
        }
        if (quantidade <= 0) {
            throw new ApplicationException(400, "A quantidade do item deve ser maior que zero");
        }
        this.id = id;
        this.descricao = descricao.trim();
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
    }

    public ValorMonetario getSubtotal() {
        return precoUnitario.multiplicar(quantidade);
    }
}
