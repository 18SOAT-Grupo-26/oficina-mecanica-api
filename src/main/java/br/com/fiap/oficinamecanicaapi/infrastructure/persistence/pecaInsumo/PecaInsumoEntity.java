package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.EntidadeComDatas;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "peca_insumo")
@Getter
@Setter
public class PecaInsumoEntity extends EntidadeComDatas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "nome_normalizado", nullable = false, unique = true)
    private String nomeNormalizado;

    private String descricao;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private Integer reservado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

}
