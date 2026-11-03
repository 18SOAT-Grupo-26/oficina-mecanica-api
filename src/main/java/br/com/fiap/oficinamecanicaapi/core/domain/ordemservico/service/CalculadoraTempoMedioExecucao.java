package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Serviço de domínio do tempo médio de execução. Recebe os tempos de execução das OS finalizadas
 * (de EM_EXECUCAO a FINALIZADA) e devolve a média aritmética; sem tempos, não há média.
 */
public final class CalculadoraTempoMedioExecucao {

    private CalculadoraTempoMedioExecucao() {
    }

    public static Optional<Duration> calcular(Collection<Duration> duracoes) {
        if (duracoes == null) {
            return Optional.empty();
        }
        List<Duration> validas = duracoes.stream().filter(Objects::nonNull).toList();
        if (validas.isEmpty()) {
            return Optional.empty();
        }
        Duration soma = validas.stream().reduce(Duration.ZERO, Duration::plus);
        return Optional.of(soma.dividedBy(validas.size()));
    }
}
