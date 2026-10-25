package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByNomeUsuario(String nomeUsuario);

    boolean existsByNomeUsuario(String nomeUsuario);

    @Query("SELECT u FROM UsuarioEntity u JOIN u.identidade i JOIN i.contato c WHERE c.email = :email")
    Optional<UsuarioEntity> buscarPorEmail(@Param("email") String email);

}