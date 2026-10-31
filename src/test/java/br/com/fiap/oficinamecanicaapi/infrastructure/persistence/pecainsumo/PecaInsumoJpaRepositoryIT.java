package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecainsumo;

import br.com.fiap.oficinamecanicaapi.integracao.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As queries nativas de estoque são a proteção contra saldo negativo; só um banco real prova as condições do WHERE.
 * Cada teste roda numa transação desfeita no fim. A leitura usa JDBC na mesma conexão, sem cache do Hibernate.
 */
@Transactional
class PecaInsumoJpaRepositoryIT extends IntegracaoBase {

    @Autowired
    private PecaInsumoJpaRepository repository;

    private Long id;

    @BeforeEach
    void inserirPeca() {
        id = inserir("Filtro de óleo", "filtro de oleo", 10, 0);
    }

    @Test
    @DisplayName("reservar deve somar ao reservado quando há saldo e recusar quando não há")
    void reservar() {
        assertThat(repository.reservar(id, 6)).isEqualTo(1);
        assertThat(repository.reservar(id, 5)).isZero();
        assertThat(repository.reservar(id, 4)).isEqualTo(1);

        assertThat(saldo()).containsExactly(10, 10);
    }

    @Test
    @DisplayName("liberar deve devolver o reservado e recusar devolver mais do que foi reservado")
    void liberar() {
        repository.reservar(id, 3);

        assertThat(repository.liberar(id, 4)).isZero();
        assertThat(repository.liberar(id, 3)).isEqualTo(1);
        assertThat(saldo()).containsExactly(10, 0);
    }

    @Test
    @DisplayName("baixar deve tirar do estoque e do reservado, e só o que estava reservado")
    void baixar() {
        repository.reservar(id, 2);

        assertThat(repository.baixar(id, 3)).isZero();
        assertThat(repository.baixar(id, 2)).isEqualTo(1);
        assertThat(saldo()).containsExactly(8, 0);
    }

    @Test
    @DisplayName("Operação em peça inexistente não deve afetar linhas")
    void pecaInexistente() {
        assertThat(repository.reservar(9999L, 1)).isZero();
    }

    @Test
    @DisplayName("contarSemSaldo deve contar só as peças ativas com disponível <= 0")
    void contarSemSaldo() {
        inserir("Vela", "vela", 0, 0);
        inserir("Pastilha", "pastilha", 4, 4);
        Long desativada = inserir("Correia", "correia", 0, 0);
        jdbcTemplate.update("update peca_insumo set ativo = false where id = ?", desativada);

        assertThat(repository.contarSemSaldo()).isEqualTo(2);
    }

    @Test
    @DisplayName("existsByNomeNormalizadoAndAtivoTrue deve achar pelo nome normalizado, só entre as peças ativas")
    void existePorNome() {
        assertThat(repository.existsByNomeNormalizadoAndAtivoTrue("filtro de oleo")).isTrue();
        assertThat(repository.existsByNomeNormalizadoAndAtivoTrue("filtro de ar")).isFalse();

        jdbcTemplate.update("update peca_insumo set ativo = false where id = ?", id);
        assertThat(repository.existsByNomeNormalizadoAndAtivoTrue("filtro de oleo")).isFalse();
    }

    @Test
    @DisplayName("O nome deve ser único só entre as peças ativas: a desativada libera o nome")
    void nomeUnicoEntreAtivas() {
        jdbcTemplate.update("update peca_insumo set ativo = false where id = ?", id);
        inserir("Filtro de óleo", "filtro de oleo", 5, 0);

        // último comando do teste: a violação aborta a transação no PostgreSQL
        assertThatThrownBy(() -> inserir("Filtro de Óleo", "filtro de oleo", 1, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long inserir(String nome, String normalizado, int quantidade, int reservado) {
        return jdbcTemplate.queryForObject("""
                insert into peca_insumo (nome, nome_normalizado, descricao, quantidade, reservado, preco, created_at, updated_at)
                values (?, ?, null, ?, ?, 25.00, now(), now()) returning id
                """, Long.class, nome, normalizado, quantidade, reservado);
    }

    private java.util.List<Integer> saldo() {
        return jdbcTemplate.queryForObject("select quantidade, reservado from peca_insumo where id = ?",
                (rs, n) -> java.util.List.of(rs.getInt("quantidade"), rs.getInt("reservado")), id);
    }
}
