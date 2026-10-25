package br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "perfis")
@Getter
@Setter
public class PerfilEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    private String descricao;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "perfil_permissoes",
            joinColumns = @JoinColumn(name = "perfil_id"),
            inverseJoinColumns = @JoinColumn(name = "permissao_id")
    )
    private Set<PermissaoEntity> permissoes = new HashSet<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}