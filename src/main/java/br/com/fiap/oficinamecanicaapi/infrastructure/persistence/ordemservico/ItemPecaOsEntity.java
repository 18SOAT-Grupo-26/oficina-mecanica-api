package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Peça ou insumo na OS. Descrição e preço são cópias do cadastro no momento da inclusão. */
@Entity
@Table(name = "item_peca_os")
@Getter
@Setter
public class ItemPecaOsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServicoEntity ordemServico;

    @Column(name = "peca_insumo_id", nullable = false)
    private Long pecaInsumoId;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private Integer quantidade;
}
