package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.IdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentityRepository
        extends JpaRepository<IdentityEntity, Long> {

    Optional<IdentityEntity> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

}
