package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identidade;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.EnderecoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoRepository
        extends JpaRepository<EnderecoEntity, Long> {
}
