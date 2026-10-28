package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.entity.ServicoEntity;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.servico.ServicoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoPersistenceAdapterTest {

    @Mock
    private ServicoRepository repository;

    @InjectMocks
    private ServicoPersistenceAdapter adapter;

    @Mock
    private ServicoEntity servico;

    @Test
    @DisplayName("salvar, buscarPorId e excluir devem delegar ao repositório")
    void crud() {
        when(repository.save(servico)).thenReturn(servico);
        when(repository.findById(1L)).thenReturn(Optional.of(servico));

        assertThat(adapter.salvar(servico)).isSameAs(servico);
        assertThat(adapter.buscarPorId(1L)).containsSame(servico);
        adapter.excluir(servico);

        verify(repository).delete(servico);
    }

    @Test
    @DisplayName("existePorNomeNormalizado deve repassar o resultado")
    void existePorNome() {
        when(repository.existsByNomeNormalizado("troca de oleo")).thenReturn(true);

        assertThat(adapter.existePorNomeNormalizado("troca de oleo")).isTrue();
    }

    @Test
    @DisplayName("listar deve paginar por id e devolver o total")
    void listar() {
        var pageable = PageRequest.of(0, 20, Sort.by("id"));
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(servico), pageable, 1));

        var pagina = adapter.listar(0, 20);

        assertThat(pagina.itens()).containsExactly(servico);
        assertThat(pagina.total()).isEqualTo(1);
    }
}
