package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.user;

import br.com.fiap.oficinamecanicaapi.core.domain.user.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository
        extends JpaRepository<RoleEntity, Long> {

    Optional<RoleEntity> findByName(String name);
}
