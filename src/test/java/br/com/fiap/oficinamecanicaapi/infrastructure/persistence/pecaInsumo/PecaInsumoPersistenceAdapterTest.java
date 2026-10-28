package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.entity.PecaInsumoEntity;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.pecaInsumo.PecaInsumoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PecaInsumoPersistenceAdapterTest {

    @Mock
    private PecaInsumoRepository repository;

    @InjectMocks
    private PecaInsumoPersistenceAdapter adapter;

    @Mock
    private PecaInsumoEntity peca;

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
    @DisplayName("salvar, buscar, buscarPorIds e excluir devem delegar ao repositório")
    void crud() {
        when(repository.save(peca)).thenReturn(peca);
        when(repository.findById(1L)).thenReturn(Optional.of(peca));
        when(repository.findAllById(Set.of(1L))).thenReturn(List.of(peca));

        assertThat(adapter.salvar(peca)).isSameAs(peca);
        assertThat(adapter.buscarPorId(1L)).containsSame(peca);
        assertThat(adapter.buscarPorIds(Set.of(1L))).containsExactly(peca);
        adapter.excluir(peca);

        verify(repository).delete(peca);
    }

    @Test
    @DisplayName("existePorNomeNormalizado e listar devem repassar os dados do repositório")
    void existeEListar() {
        var pageable = PageRequest.of(0, 20, Sort.by("id"));
        when(repository.existsByNomeNormalizado("filtro de oleo")).thenReturn(false);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(peca), pageable, 1));

        assertThat(adapter.existePorNomeNormalizado("filtro de oleo")).isFalse();
        assertThat(adapter.listar(0, 20).itens()).containsExactly(peca);
    }
}
