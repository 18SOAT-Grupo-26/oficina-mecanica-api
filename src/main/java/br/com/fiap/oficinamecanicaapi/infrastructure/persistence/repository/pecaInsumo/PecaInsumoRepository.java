package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.entity.PecaInsumoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PecaInsumoRepository
        extends JpaRepository<PecaInsumoEntity, Long> {

    boolean existsByNomeNormalizado(String nomeNormalizado);
}
