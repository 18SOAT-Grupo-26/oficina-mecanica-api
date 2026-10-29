package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthenticationUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecaInsumo.PecaInsumoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.auth.JwtTokenProviderUseCase;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guarda de segurança: toda rota da aplicação que não estiver em {@link RotasPublicas} precisa responder 401
 * sem token e com token inválido. Sobe todos os controllers ({@code @WebMvcTest} sem lista), então um
 * controller novo exige o mock da sua porta aqui, o que obriga a revisar a segurança dele.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class RotasProtegidasTest {

    private static final String PACOTE_DA_APLICACAO = "br.com.fiap.oficinamecanicaapi";
    private static final String TOKEN_INVALIDO = "token-invalido";
    /**
     * CRUDs de cliente, veículo, insumo e serviço, operações da OS e as duas rotas do cliente na data do teste.
     */
    private static final int ROTAS_PROTEGIDAS_MINIMO = 28;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @MockitoBean
    private ClienteUseCasePortIn clienteUseCase;

    @MockitoBean
    private VeiculoUseCasePortIn veiculoUseCase;

    @MockitoBean
    private PecaInsumoUseCasePortIn pecaInsumoUseCase;

    @MockitoBean
    private ServicoUseCasePortIn servicoUseCase;

    @MockitoBean
    private OrdemServicoUseCasePortIn ordemServicoUseCase;

    @MockitoBean
    private AuthenticationUseCasePortIn authenticationUseCase;

    @MockitoBean
    private JwtTokenProviderUseCase jwtTokenProvider;

    @MockitoBean
    private UsuarioUseCasePortIn usuarioUseCase;

    @BeforeEach
    void configurarTokenInvalido() {
        when(jwtTokenProvider.validarToken(TOKEN_INVALIDO))
                .thenThrow(new ApplicationException(401, "Token inválido"));
    }

    @Test
    @DisplayName("Deve encontrar todas as rotas protegidas, evitando um guarda que passa com lista vazia")
    void deveEncontrarRotasProtegidas() {
        assertThat(rotasProtegidas()).hasSizeGreaterThanOrEqualTo(ROTAS_PROTEGIDAS_MINIMO);
    }

    @TestFactory
    @DisplayName("Toda rota não pública deve responder 401 sem token")
    Stream<DynamicTest> deveExigirTokenEmTodasAsRotasProtegidas() {
        return rotasProtegidas().stream().map(rota -> DynamicTest.dynamicTest(rota + " sem token",
                () -> mockMvc.perform(request(rota.metodo(), rota.caminho()))
                        .andExpect(status().isUnauthorized())));
    }

    @TestFactory
    @DisplayName("Toda rota não pública deve responder 401 com token inválido")
    Stream<DynamicTest> deveRecusarTokenInvalidoEmTodasAsRotasProtegidas() {
        return rotasProtegidas().stream().map(rota -> DynamicTest.dynamicTest(rota + " com token inválido",
                () -> mockMvc.perform(request(rota.metodo(), rota.caminho())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_INVALIDO))
                        .andExpect(status().isUnauthorized())));
    }

    @Test
    @DisplayName("Validação de token sem header Authorization deve responder 400, e não 500")
    void validacaoDeTokenSemHeaderDeveResponder400() throws Exception {
        mockMvc.perform(get("/auth/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header obrigatório ausente: Authorization"));
    }

    @Test
    @DisplayName("Toda resposta deve enviar Cross-Origin-Resource-Policy same-origin (isolamento contra Spectre)")
    void deveEnviarCrossOriginResourcePolicy() throws Exception {
        mockMvc.perform(get("/auth/validate"))
                .andExpect(header().string("Cross-Origin-Resource-Policy", "same-origin"));
    }

    private List<Rota> rotasProtegidas() {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entrada -> entrada.getValue().getBeanType().getPackageName().startsWith(PACOTE_DA_APLICACAO))
                .flatMap(entrada -> rotas(entrada.getKey()))
                .filter(rota -> !RotasPublicas.isPublica(rota.padrao()))
                .distinct()
                .sorted((a, b) -> a.toString().compareTo(b.toString()))
                .toList();
    }

    private static Stream<Rota> rotas(RequestMappingInfo info) {
        Set<RequestMethod> metodos = info.getMethodsCondition().getMethods();
        Set<RequestMethod> considerados = metodos.isEmpty() ? Set.of(RequestMethod.GET) : metodos;
        return info.getPatternValues().stream()
                .flatMap(padrao -> considerados.stream()
                        .map(metodo -> new Rota(HttpMethod.valueOf(metodo.name()), padrao)));
    }

    private record Rota(HttpMethod metodo, String padrao) {

        /**
         * Variáveis de caminho viram 1 só para montar uma URL válida; a segurança barra antes do controller.
         */
        String caminho() {
            return padrao.replaceAll("\\{[^}]+}", "1");
        }

        @Override
        public String toString() {
            return metodo + " " + padrao;
        }
    }
}
