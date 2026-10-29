package br.com.fiap.oficinamecanicaapi.infrastructure.config.metrics;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico.OrdemServicoJpaRepository;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.pecainsumo.PecaInsumoRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OficinaMetricasConfigTest {

    @Mock
    private OrdemServicoJpaRepository ordemServicoRepository;

    @Mock
    private PecaInsumoRepository pecaInsumoRepository;

    private SimpleMeterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        new OficinaMetricasConfig(ordemServicoRepository, pecaInsumoRepository, registry);
    }

    @Test
    @DisplayName("Deve registrar um gauge de OS por status, sem consultar o banco no registro")
    void deveRegistrarUmGaugePorStatus() {
        assertThat(registry.find("oficina.os.quantidade").gauges()).hasSize(StatusOrdemServico.values().length);
        verifyNoInteractions(ordemServicoRepository, pecaInsumoRepository);
    }

    @Test
    @DisplayName("Gauge de OS deve ler a contagem do status na hora da coleta")
    void deveLerQuantidadePorStatus() {
        when(ordemServicoRepository.countByStatus(StatusOrdemServico.EM_EXECUCAO)).thenReturn(3L, 5L);

        var gauge = registry.get("oficina.os.quantidade").tag("status", "EM_EXECUCAO").gauge();

        assertThat(gauge.value()).isEqualTo(3.0);
        assertThat(gauge.value()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Gauge de estoque deve ler a quantidade de peças sem saldo")
    void deveLerPecasSemSaldo() {
        when(pecaInsumoRepository.contarSemSaldo()).thenReturn(2L);

        assertThat(registry.get("oficina.estoque.pecas.sem.saldo").gauge().value()).isEqualTo(2.0);
    }
}
