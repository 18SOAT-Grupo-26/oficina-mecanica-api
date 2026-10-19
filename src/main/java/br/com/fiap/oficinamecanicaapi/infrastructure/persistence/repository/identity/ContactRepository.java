package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.ContactEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.IdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContactRepository extends JpaRepository<ContactEntity, Long> {

    @Query("SELECT i FROM IdentityEntity i JOIN i.contact c WHERE c.email = :email")
    Optional<IdentityEntity> findByContactEmail(@Param("email") String email);

    boolean existsByEmail(String email);

}