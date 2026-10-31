package br.com.fiap.oficinamecanicaapi.infrastructure.config.metrics;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico.OrdemServicoJpaRepository;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecainsumo.PecaInsumoJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * Métricas de estado (gauges), lidas do banco a cada coleta do Prometheus (15 s).
 */
@Configuration
public class OficinaMetricasConfig {

    public OficinaMetricasConfig(OrdemServicoJpaRepository ordemServicoRepository,
                                 PecaInsumoJpaRepository pecaInsumoRepository,
                                 MeterRegistry registry) {
        for (StatusOrdemServico status : StatusOrdemServico.values()) {
            Gauge.builder("oficina.os.quantidade", ordemServicoRepository, repo -> repo.countByStatus(status))
                    .description("Quantidade de OS por status")
                    .tag("status", status.name())
                    .register(registry);
        }
        Gauge.builder("oficina.estoque.pecas.sem.saldo", pecaInsumoRepository, PecaInsumoJpaRepository::contarSemSaldo)
                .description("Peças com quantidade em estoque menos reservada <= 0")
                .register(registry);
    }
}