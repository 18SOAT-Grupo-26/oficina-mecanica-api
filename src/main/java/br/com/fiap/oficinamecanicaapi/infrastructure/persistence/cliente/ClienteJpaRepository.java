package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente;

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
