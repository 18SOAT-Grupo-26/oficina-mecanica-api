package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.servico;

import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.servico.ServicoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository
        extends JpaRepository<ServicoEntity, Long> {

    boolean existsByNomeNormalizado(String nomeNormalizado);

    Page<ServicoEntity> findAll(Pageable pageable);
}
