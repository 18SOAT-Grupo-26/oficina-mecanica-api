package br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.TipoDocumento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "identidades")
@Getter
@Setter
public class IdentidadeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome_completo")
    private String nomeCompleto;

    @Column(name = "primeiro_nome")
    private String primeiroNome;

    @Column(nullable = false, unique = true, length = 14)
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 4)
    private TipoDocumento tipoDocumento;

    private String rg;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "contato_id")
    private ContatoEntity contato;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "endereco_id")
    private EnderecoEntity endereco;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    private boolean ativo;

}
