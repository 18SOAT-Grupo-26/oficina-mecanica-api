package br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.IdentidadeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "identidade_id")
    private IdentidadeEntity identidade;

    @Column(name = "nome_usuario")
    private String nomeUsuario;

    private String senha;

    @Column(name = "troca_senha_obrigatoria", nullable = false, columnDefinition = "boolean default false")
    private boolean trocaSenhaObrigatoria = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilEntity perfil;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    private boolean ativo;
}