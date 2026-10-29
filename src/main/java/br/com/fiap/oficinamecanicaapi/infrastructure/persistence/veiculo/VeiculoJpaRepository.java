package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.veiculo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VeiculoJpaRepository extends JpaRepository<VeiculoEntity, Long> {

    Optional<VeiculoEntity> findByIdAndAtivoTrue(Long id);

    Optional<VeiculoEntity> findByPlacaAndAtivoTrue(String placa);

    boolean existsByPlaca(String placa);

    Page<VeiculoEntity> findAllByAtivoTrue(Pageable pageable);

    Page<VeiculoEntity> findAllByClienteIdAndAtivoTrue(Long clienteId, Pageable pageable);
}
