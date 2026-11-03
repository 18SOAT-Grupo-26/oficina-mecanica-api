package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.*;
import static org.assertj.core.api.Assertions.assertThat;

class StatusOrdemServicoTest {

    /**
     * Tabela de transições acordada com o grupo: a OS só avança para o próximo status e pode ser
     * cancelada até AGUARDANDO_APROVACAO. Qualquer par fora dela é inválido.
     */
    private static final Map<StatusOrdemServico, Set<StatusOrdemServico>> TRANSICOES_VALIDAS = Map.of(
            RECEBIDA, Set.of(EM_DIAGNOSTICO, CANCELADA),
            EM_DIAGNOSTICO, Set.of(AGUARDANDO_APROVACAO, CANCELADA),
            AGUARDANDO_APROVACAO, Set.of(EM_EXECUCAO, CANCELADA),
            EM_EXECUCAO, Set.of(FINALIZADA),
            FINALIZADA, Set.of(ENTREGUE),
            ENTREGUE, Set.of(),
            CANCELADA, Set.of()
    );

    @Test
    @DisplayName("Deve ter os seis status do enunciado e o status CANCELADA")
    void deveTerOsSeteStatus() {
        assertThat(StatusOrdemServico.values()).containsExactly(
                RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, EM_EXECUCAO, FINALIZADA, ENTREGUE, CANCELADA);
    }

    @Test
    @DisplayName("Deve permitir ir de RECEBIDA para EM_DIAGNOSTICO")
    void devePermitirIrDeRecebidaParaEmDiagnostico() {
        assertThat(RECEBIDA.podeTransitarPara(EM_DIAGNOSTICO)).isTrue();
    }

    @Test
    @DisplayName("Deve permitir cancelar só até AGUARDANDO_APROVACAO")
    void devePermitirCancelarSoAteAguardandoAprovacao() {
        assertThat(Arrays.stream(StatusOrdemServico.values()).filter(status -> status.podeTransitarPara(CANCELADA)))
                .containsExactly(RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO);
    }

    @Test
    @DisplayName("Deve permitir editar a OS só antes de EM_EXECUCAO, e nunca cancelada")
    void devePermitirEditarSoAntesDeEmExecucao() {
        assertThat(Arrays.stream(StatusOrdemServico.values()).filter(StatusOrdemServico::isEditavel))
                .containsExactly(RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO);
    }

    @ParameterizedTest(name = "{0} → {1}: permitido = {2}")
    @MethodSource("todosOsPares")
    @DisplayName("Deve permitir só as transições da tabela e recusar pular, voltar ou repetir etapa")
    void deveSeguirTabelaDeTransicoes(StatusOrdemServico origem, StatusOrdemServico destino, boolean permitido) {
        assertThat(origem.podeTransitarPara(destino)).isEqualTo(permitido);
    }

    @Test
    @DisplayName("Deve recusar destino nulo")
    void deveRecusarDestinoNulo() {
        assertThat(RECEBIDA.podeTransitarPara(null)).isFalse();
    }

    @Test
    @DisplayName("Deve tratar ENTREGUE e CANCELADA como status finais")
    void deveTratarEntregueECanceladaComoFinais() {
        assertThat(Arrays.stream(StatusOrdemServico.values()).filter(StatusOrdemServico::isFinal))
                .containsExactlyInAnyOrder(ENTREGUE, CANCELADA);
    }

    private static Stream<Arguments> todosOsPares() {
        return Arrays.stream(StatusOrdemServico.values())
                .flatMap(origem -> Arrays.stream(StatusOrdemServico.values())
                        .map(destino -> Arguments.of(origem, destino,
                                TRANSICOES_VALIDAS.get(origem).contains(destino))));
    }
}
