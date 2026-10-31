package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoPersistenceAdapterTest {

    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-20T10:00:00-03:00");

    @Mock
    private ServicoJpaRepository repository;

    private ServicoPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ServicoPersistenceAdapter(repository, Mappers.getMapper(ServicoPersistenceMapper.class));
    }

    @Test
    @DisplayName("salvar deve converter o domínio em entidade, preservando id e datas, e devolver o domínio salvo")
    void salvar() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Servico servico = Servico.reconstituir(
                1L,
                "Troca de óleo",
                null,
                null,
                new BigDecimal("80.00"),
                CRIADO_EM,
                null);

        Servico salvo = adapter.salvar(servico);

        ArgumentCaptor<ServicoEntity> captor = ArgumentCaptor.forClass(ServicoEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(CRIADO_EM);
        assertThat(salvo.getNome()).isEqualTo("Troca de óleo");
        assertThat(salvo.getCriadoEm()).isEqualTo(CRIADO_EM);
    }

    @Test
    @DisplayName("buscarPorId deve converter a entidade em domínio e excluir deve apagar pelo id")
    void buscarEExcluir() {
        when(repository.findById(1L)).thenReturn(Optional.of(entidade()));

        Servico servico = adapter.buscarPorId(1L).orElseThrow();
        adapter.excluir(servico);

        assertThat(servico.getNome()).isEqualTo("Troca de óleo");
        assertThat(servico.getCriadoEm()).isEqualTo(CRIADO_EM);
        verify(repository).deleteById(1L);
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
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entidade()), pageable, 1));

        var pagina = adapter.listar(1, 20);

        assertThat(pagina.itens()).extracting(Servico::getId).containsExactly(1L);
        assertThat(pagina.total()).isEqualTo(1);
    }

    private static ServicoEntity entidade() {
        ServicoEntity entity = new ServicoEntity();
        entity.setId(1L);
        entity.setNome("Troca de óleo");
        entity.setPreco(new BigDecimal("80.00"));
        entity.setCreatedAt(CRIADO_EM);
        return entity;
    }
}
