package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecainsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.pecainsumo.PecaInsumoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PecaInsumoPersistenceAdapterTest {

    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-20T10:00:00-03:00");

    @Mock
    private PecaInsumoRepository repository;

    private PecaInsumoPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PecaInsumoPersistenceAdapter(repository, Mappers.getMapper(PecaInsumoPersistenceMapper.class));
    }

    @ParameterizedTest(name = "linhas afetadas = {0} → {1}")
    @CsvSource({"1,true", "0,false"})
    @DisplayName("reservar, liberar e baixar só retornam true quando exatamente uma linha foi atualizada")
    void operacoesDeEstoque(int linhas, boolean esperado) {
        when(repository.reservar(1L, 2)).thenReturn(linhas);
        when(repository.liberar(1L, 2)).thenReturn(linhas);
        when(repository.baixar(1L, 2)).thenReturn(linhas);

        assertThat(adapter.reservar(1L, 2)).isEqualTo(esperado);
        assertThat(adapter.liberar(1L, 2)).isEqualTo(esperado);
        assertThat(adapter.baixar(1L, 2)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("salvar deve converter o domínio em entidade, preservando id, estoque e datas")
    void salvar() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PecaInsumo peca = new PecaInsumo();
        peca.setId(1L);
        peca.setNome("Filtro de óleo");
        peca.setQuantidade(10);
        peca.setReservado(2);
        peca.setCriadoEm(CRIADO_EM);

        PecaInsumo salvo = adapter.salvar(peca);

        ArgumentCaptor<PecaInsumoEntity> captor = ArgumentCaptor.forClass(PecaInsumoEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReservado()).isEqualTo(2);
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(CRIADO_EM);
        assertThat(salvo.getQuantidade()).isEqualTo(10);
        assertThat(salvo.getCriadoEm()).isEqualTo(CRIADO_EM);
    }

    @Test
    @DisplayName("buscarPorId e buscarPorIds devem converter em domínio e excluir deve apagar pelo id")
    void buscarEExcluir() {
        when(repository.findById(1L)).thenReturn(Optional.of(entidade()));
        when(repository.findAllById(Set.of(1L))).thenReturn(List.of(entidade()));

        PecaInsumo peca = adapter.buscarPorId(1L).orElseThrow();
        assertThat(adapter.buscarPorIds(Set.of(1L))).extracting(PecaInsumo::getId).containsExactly(1L);
        adapter.excluir(peca);

        assertThat(peca.getCriadoEm()).isEqualTo(CRIADO_EM);
        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("existePorNomeNormalizado e listar devem repassar os dados do repositório")
    void existeEListar() {
        var pageable = PageRequest.of(0, 20, Sort.by("id"));
        when(repository.existsByNomeNormalizado("filtro de oleo")).thenReturn(false);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entidade()), pageable, 1));

        assertThat(adapter.existePorNomeNormalizado("filtro de oleo")).isFalse();
        assertThat(adapter.listar(1, 20).itens()).extracting(PecaInsumo::getId).containsExactly(1L);
    }

    private static PecaInsumoEntity entidade() {
        PecaInsumoEntity entity = new PecaInsumoEntity();
        entity.setId(1L);
        entity.setNome("Filtro de óleo");
        entity.setQuantidade(10);
        entity.setReservado(0);
        entity.setCreatedAt(CRIADO_EM);
        return entity;
    }
}
