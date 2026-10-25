package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identidade;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.ContatoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.IdentidadeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContatoRepository extends JpaRepository<ContatoEntity, Long> {

    @Query("SELECT i FROM IdentidadeEntity i JOIN i.contato c WHERE c.email = :email")
    Optional<IdentidadeEntity> buscarIdentidadePorEmail(@Param("email") String email);

    boolean existsByEmail(String email);

}