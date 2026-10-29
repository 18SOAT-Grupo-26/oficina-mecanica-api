package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.usuario;

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

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "nome_usuario", unique = true)
    private String nomeUsuario;

    private String senha;

    @Column(name = "troca_senha_obrigatoria", nullable = false, columnDefinition = "boolean default false")
    private boolean trocaSenhaObrigatoria = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilEntity perfil;

    @Column(name = "cliente_id", unique = true)
    private Long clienteId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    private boolean ativo;
}