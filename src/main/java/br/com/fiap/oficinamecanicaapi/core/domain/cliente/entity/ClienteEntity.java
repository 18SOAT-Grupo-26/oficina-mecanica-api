package br.com.fiap.oficinamecanicaapi.core.domain.cliente.entity;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.TipoDocumento;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente.EnderecoEmbeddable;
import br.com.fiap.oficinamecanicaapi.shared.persistence.EntidadeComDatas;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "clientes")
@Getter
@Setter
public class ClienteEntity extends EntidadeComDatas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 4)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telefone;

    @Embedded
    private EnderecoEmbeddable endereco;

    @Column(nullable = false)
    private boolean ativo;

}
