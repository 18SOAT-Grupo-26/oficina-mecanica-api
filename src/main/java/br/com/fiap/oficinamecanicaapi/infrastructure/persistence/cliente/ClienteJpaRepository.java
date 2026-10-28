package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.entity.ClienteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {

    Optional<ClienteEntity> findByIdAndAtivoTrue(Long id);

    Optional<ClienteEntity> findByDocumentoAndAtivoTrue(String documento);

    boolean existsByDocumento(String documento);

    Page<ClienteEntity> findAllByAtivoTrue(Pageable pageable);
}
