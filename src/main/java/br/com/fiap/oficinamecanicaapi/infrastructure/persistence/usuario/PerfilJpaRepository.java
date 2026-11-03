package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilJpaRepository
        extends JpaRepository<PerfilEntity, Long> {

    Optional<PerfilEntity> findByNome(String nome);
}
