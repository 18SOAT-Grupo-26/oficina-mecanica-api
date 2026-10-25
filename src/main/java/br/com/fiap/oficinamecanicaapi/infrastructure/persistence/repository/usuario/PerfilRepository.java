package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.PerfilEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilRepository
        extends JpaRepository<PerfilEntity, Long> {

    Optional<PerfilEntity> findByNome(String nome);
}
