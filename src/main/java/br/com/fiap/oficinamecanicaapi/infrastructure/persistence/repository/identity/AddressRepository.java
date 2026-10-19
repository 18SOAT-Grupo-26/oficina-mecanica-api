package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository
        extends JpaRepository<AddressEntity, Long> {
}
