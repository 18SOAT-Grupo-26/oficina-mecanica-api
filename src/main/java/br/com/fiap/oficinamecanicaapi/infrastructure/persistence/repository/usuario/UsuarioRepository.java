package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository
        extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByNomeUsuario(String nomeUsuario);

    boolean existsByNomeUsuario(String nomeUsuario);

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPerfil_Nome(String nome);

}