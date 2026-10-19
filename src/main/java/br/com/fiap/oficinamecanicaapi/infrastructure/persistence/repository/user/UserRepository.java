package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.user;

import br.com.fiap.oficinamecanicaapi.core.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM UserEntity u JOIN u.identity i JOIN i.contact c WHERE c.email = :email")
    Optional<UserEntity> findByEmail(@Param("email") String email);

}