package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.servico;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoJpaRepository
        extends JpaRepository<ServicoEntity, Long> {

    boolean existsByNomeNormalizado(String nomeNormalizado);

    Page<ServicoEntity> findAll(Pageable pageable);
}
