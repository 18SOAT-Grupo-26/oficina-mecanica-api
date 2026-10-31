package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.servico;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServicoJpaRepository
        extends JpaRepository<ServicoEntity, Long> {

    Optional<ServicoEntity> findByIdAndAtivoTrue(Long id);

    boolean existsByNomeNormalizadoAndAtivoTrue(String nomeNormalizado);

    Page<ServicoEntity> findAllByAtivoTrue(Pageable pageable);
}
