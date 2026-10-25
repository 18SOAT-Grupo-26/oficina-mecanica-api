package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.PermissaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissaoRepository
        extends JpaRepository<PermissaoEntity, Long> {

    Optional<PermissaoEntity> findByNome(String nome);
}
