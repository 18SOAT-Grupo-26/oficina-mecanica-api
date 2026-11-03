package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RotasPublicasTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/auth/login", "/auth/validacao",
            "/actuator/health", "/actuator/prometheus",
            "/documentation", "/swagger-ui/index.html", "/v3/api-docs", "/api-docs-personalized"})
    @DisplayName("Deve tratar como públicas só autenticação, documentação e monitoramento")
    void deveReconhecerRotasPublicas(String rota) {
        assertThat(RotasPublicas.isPublica(rota)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/clientes", "/clientes/1", "/veiculos", "/servicos", "/pecas-insumos",
            "/ordens-servico", "/ordens-servico/1/status", "/ordens-servico/tempo-medio-execucao",
            "/usuarios", "/auth/cadastro", "/auth/usuarios", "/authx", "/autenticacao/login"})
    @DisplayName("Deve exigir token em todas as outras rotas, inclusive o cadastro e qualquer rota nova em /auth")
    void deveReconhecerRotasProtegidas(String rota) {
        assertThat(RotasPublicas.isPublica(rota)).isFalse();
    }

    @Test
    @DisplayName("Deve juntar os padrões de autenticação, documentação e monitoramento")
    void deveListarTodosOsPadroes() {
        assertThat(RotasPublicas.todas())
                .containsAll(List.of(RotasPublicas.AUTENTICACAO))
                .containsAll(List.of(RotasPublicas.DOCUMENTACAO))
                .containsAll(List.of(RotasPublicas.MONITORAMENTO))
                .hasSize(RotasPublicas.AUTENTICACAO.length + RotasPublicas.DOCUMENTACAO.length
                        + RotasPublicas.MONITORAMENTO.length);
    }
}
