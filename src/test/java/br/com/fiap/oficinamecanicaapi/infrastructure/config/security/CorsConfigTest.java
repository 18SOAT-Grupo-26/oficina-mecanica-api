package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import br.com.fiap.oficinamecanicaapi.controller.admin.ClienteController;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.auth.JwtTokenProviderUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS: só as origens de api.cors.allowed-origins (CORS_ALLOWED_ORIGINS) chamam a API pelo navegador.
 */
@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "api.cors.allowed-origins=" + CorsConfigTest.FRONT_LOCAL + ", " + CorsConfigTest.FRONT_PRODUCAO)
class CorsConfigTest {

    static final String FRONT_LOCAL = "http://localhost:3000";
    static final String FRONT_PRODUCAO = "https://app.oficina.com";
    private static final String ORIGEM_MALICIOSA = "https://site-malicioso.com";
    private static final String ROTA = "/clientes";
    private static final String ROTA_STATUS_OS = "/ordens-servico/1/status";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteUseCasePortIn clienteUseCase;

    @MockitoBean
    private JwtTokenProviderUseCase jwtTokenProvider;

    @MockitoBean
    private UsuarioUseCasePortIn usuarioUseCase;

    @Test
    @DisplayName("Deve liberar o preflight de origem permitida, sem enviar credenciais")
    void deveLiberarOrigemPermitida() throws Exception {
        mockMvc.perform(preflight(FRONT_LOCAL, "GET", ROTA))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_LOCAL))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    @DisplayName("Deve aceitar origem listada com espaço após a vírgula")
    void deveAceitarOrigemComEspacoNaLista() throws Exception {
        mockMvc.perform(preflight(FRONT_PRODUCAO, "GET", ROTA))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_PRODUCAO));
    }

    @Test
    @DisplayName("Deve recusar com 403 o preflight de origem não permitida")
    void deveRecusarOrigemNaoPermitida() throws Exception {
        mockMvc.perform(preflight(ORIGEM_MALICIOSA, "GET", ROTA))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Deve liberar PATCH, usado na troca de status da OS")
    void deveLiberarPatch() throws Exception {
        mockMvc.perform(preflight(FRONT_LOCAL, "PATCH", ROTA_STATUS_OS))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")));
    }

    @Test
    @DisplayName("Sem CORS_ALLOWED_ORIGINS, nenhuma origem deve ser liberada")
    void naoDeveLiberarNenhumaOrigemSemConfiguracao() {
        CorsConfiguration config = new SecurityConfig(null, "")
                .corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", ROTA));

        assertThat(config).isNotNull();
        assertThat(config.checkOrigin(FRONT_LOCAL)).isNull();
        assertThat(config.checkOrigin(ORIGEM_MALICIOSA)).isNull();
    }

    private static MockHttpServletRequestBuilder preflight(
            String origem, String metodo, String rota) {
        return options(rota)
                .header(HttpHeaders.ORIGIN, origem)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, metodo)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization");
    }
}
