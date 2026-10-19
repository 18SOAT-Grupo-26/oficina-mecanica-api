package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.user;

import br.com.fiap.oficinamecanicaapi.core.domain.user.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository
        extends JpaRepository<PermissionEntity, Long> {

    Optional<PermissionEntity> findByName(String name);
}
