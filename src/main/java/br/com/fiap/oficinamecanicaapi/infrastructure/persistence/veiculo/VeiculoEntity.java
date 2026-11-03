package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.veiculo;

import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente.ClienteEntity;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.EntidadeComDatas;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "veiculos")
@Getter
@Setter
public class VeiculoEntity extends EntidadeComDatas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 7)
    private String placa;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private Integer ano;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @Column(nullable = false)
    private boolean ativo;

}
