package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecainsumo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PecaInsumoJpaRepository
        extends JpaRepository<PecaInsumoEntity, Long> {

    @Modifying
    @Query(value = """
            UPDATE peca_insumo
            SET reservado = reservado + :quantidade
            WHERE id = :id AND quantidade - reservado >= :quantidade
            """, nativeQuery = true)
    int reservar(@Param("id") Long id, @Param("quantidade") int quantidade);

    @Modifying
    @Query(value = """
            UPDATE peca_insumo
            SET reservado = reservado - :quantidade
            WHERE id = :id AND reservado >= :quantidade
            """, nativeQuery = true)
    int liberar(@Param("id") Long id, @Param("quantidade") int quantidade);

    @Modifying
    @Query(value = """
            UPDATE peca_insumo
            SET quantidade = quantidade - :quantidade, reservado = reservado - :quantidade
            WHERE id = :id AND reservado >= :quantidade AND quantidade >= :quantidade
            """, nativeQuery = true)
    int baixar(@Param("id") Long id, @Param("quantidade") int quantidade);

    Optional<PecaInsumoEntity> findByIdAndAtivoTrue(Long id);

    boolean existsByNomeNormalizado(String nomeNormalizado);

    Page<PecaInsumoEntity> findAllByAtivoTrue(Pageable pageable);


    @Query("select count(p) from PecaInsumoEntity p where p.ativo = true and p.quantidade - p.reservado <= 0")
    long contarSemSaldo();
}
