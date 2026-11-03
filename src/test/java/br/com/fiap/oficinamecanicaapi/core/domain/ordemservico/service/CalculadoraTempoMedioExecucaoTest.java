package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraTempoMedioExecucaoTest {

    @Test
    @DisplayName("Deve calcular a média das durações")
    void deveCalcularMedia() {
        assertThat(CalculadoraTempoMedioExecucao.calcular(
                List.of(Duration.ofHours(1), Duration.ofHours(2), Duration.ofHours(3))))
                .contains(Duration.ofHours(2));
    }

    @Test
    @DisplayName("Deve retornar vazio, e não zero, quando não há durações")
    void deveRetornarVazioSemDuracoes() {
        assertThat(CalculadoraTempoMedioExecucao.calcular(List.of())).isEmpty();
        assertThat(CalculadoraTempoMedioExecucao.calcular(null)).isEmpty();
    }

    @Test
    @DisplayName("Deve ignorar durações nulas no cálculo")
    void deveIgnorarDuracoesNulas() {
        assertThat(CalculadoraTempoMedioExecucao.calcular(Arrays.asList(Duration.ofMinutes(30), null)))
                .contains(Duration.ofMinutes(30));
    }
}
