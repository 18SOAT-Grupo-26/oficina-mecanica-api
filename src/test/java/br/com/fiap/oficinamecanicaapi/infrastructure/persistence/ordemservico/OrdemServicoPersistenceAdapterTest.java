package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServico;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemServicoPersistenceAdapterTest {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-10-25T08:00:00-03:00");
    private static final Long ID_OS = 99L;

    @Mock
    private OrdemServicoJpaRepository repository;

    @Captor
    private ArgumentCaptor<OrdemServicoEntity> entityCaptor;

    private OrdemServicoPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OrdemServicoPersistenceAdapter(repository, new OrdemServicoPersistenceMapper());
    }

    @Test
    @DisplayName("Deve inserir a OS nova e devolver o agregado com o id gerado")
    void deveInserirOsNova() {
        when(repository.saveAndFlush(any(OrdemServicoEntity.class))).thenAnswer(invocacao -> {
            OrdemServicoEntity entity = invocacao.getArgument(0);
            entity.setId(ID_OS);
            entity.getHistorico().getFirst().setIdHistorico(1L);
            return entity;
        });

        OrdemServico salva = adapter.salvar(OrdemServico.abrir(10L, 20L, T0));

        verify(repository, never()).findById(any());
        verify(repository).saveAndFlush(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getStatus()).isEqualTo(EM_DIAGNOSTICO);
        assertThat(salva.getId()).isEqualTo(ID_OS);
        assertThat(salva.getStatus()).isEqualTo(EM_DIAGNOSTICO);
    }

    @Test
    @DisplayName("Deve atualizar a OS existente sobre a entidade carregada, sem criar outra")
    void deveAtualizarOsExistente() {
        OrdemServicoEntity existente = OrdemServicoPersistenceMapperTest.entidadeEmExecucao();
        when(repository.findById(ID_OS)).thenReturn(Optional.of(existente));
        when(repository.saveAndFlush(existente)).thenReturn(existente);
        OrdemServico os = new OrdemServicoPersistenceMapper().toDomain(OrdemServicoPersistenceMapperTest.entidadeEmExecucao());
        os.finalizar(T0.plusHours(5));

        OrdemServico salva = adapter.salvar(os);

        assertThat(existente.getStatus()).isEqualTo(FINALIZADA);
        assertThat(existente.getHistorico()).hasSize(5);
        assertThat(salva.getStatus()).isEqualTo(FINALIZADA);
    }

    @Test
    @DisplayName("Deve responder 404 ao gravar uma OS com id que não existe no banco")
    void deveResponder404ParaOsInexistente() {
        when(repository.findById(ID_OS)).thenReturn(Optional.empty());
        OrdemServico os = new OrdemServicoPersistenceMapper().toDomain(OrdemServicoPersistenceMapperTest.entidadeEmExecucao());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class, () -> adapter.salvar(os));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve buscar a OS pelo id e reconstituir o agregado")
    void deveBuscarPorId() {
        when(repository.findById(ID_OS)).thenReturn(Optional.of(OrdemServicoPersistenceMapperTest.entidadeEmExecucao()));

        assertThat(adapter.buscarPorId(ID_OS)).hasValueSatisfying(os -> {
            assertThat(os.getId()).isEqualTo(ID_OS);
            assertThat(os.getStatus()).isEqualTo(EM_EXECUCAO);
        });
    }

    @Test
    @DisplayName("Deve devolver vazio quando a OS não existe")
    void deveDevolverVazioSemOs() {
        when(repository.findById(ID_OS)).thenReturn(Optional.empty());

        assertThat(adapter.buscarPorId(ID_OS)).isEmpty();
    }

    @Test
    @DisplayName("Deve listar o tempo de EM_EXECUCAO até FINALIZADA de cada OS finalizada")
    void deveListarTemposExecucao() {
        when(repository.listarPeriodosExecucao()).thenReturn(List.of(
                new PeriodoExecucao(T0, T0.plusMinutes(90)),
                new PeriodoExecucao(T0, T0.plusHours(3))));

        assertThat(adapter.listarTemposExecucao())
                .containsExactly(Duration.ofMinutes(90), Duration.ofHours(3));
    }
}
