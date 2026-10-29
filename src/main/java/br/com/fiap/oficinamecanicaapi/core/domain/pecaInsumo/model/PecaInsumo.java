package br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Peça ou insumo do estoque, sem JPA. A entidade fica em {@code infrastructure/persistence/pecaInsumo}.
 */
@Getter
@Setter
public class PecaInsumo {

    private Long id;
    private String nome;
    private String nomeNormalizado;
    private String descricao;
    private Integer quantidade;
    private Integer reservado;
    private BigDecimal preco;
    private OffsetDateTime criadoEm;
    private OffsetDateTime atualizadoEm;
}
