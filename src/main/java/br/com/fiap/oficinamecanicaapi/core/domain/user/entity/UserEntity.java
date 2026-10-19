package br.com.fiap.oficinamecanicaapi.core.domain.user.entity;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.IdentityEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "identity_id")   // corrigido: era "identities_id"
    private IdentityEntity identity;

    private String username;
    private String password;

    @Column(name = "change_required", nullable = false, columnDefinition = "boolean default false")
    private boolean changeRequired = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    private boolean active;
}