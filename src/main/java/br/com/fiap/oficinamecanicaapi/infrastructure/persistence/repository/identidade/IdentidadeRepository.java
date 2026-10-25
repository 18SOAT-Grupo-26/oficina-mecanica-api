package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identidade;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.IdentidadeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentidadeRepository
        extends JpaRepository<IdentidadeEntity, Long> {

    Optional<IdentidadeEntity> findByDocumento(String documento);

    boolean existsByDocumento(String documento);

}
