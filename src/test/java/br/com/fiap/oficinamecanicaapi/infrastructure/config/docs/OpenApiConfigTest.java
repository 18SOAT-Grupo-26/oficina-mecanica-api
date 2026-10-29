package br.com.fiap.oficinamecanicaapi.infrastructure.config.docs;

import br.com.fiap.oficinamecanicaapi.controller.admin.ClienteController;
import br.com.fiap.oficinamecanicaapi.controller.admin.PecaInsumoController;
import br.com.fiap.oficinamecanicaapi.controller.admin.ServicoController;
import br.com.fiap.oficinamecanicaapi.controller.admin.VeiculoController;
import br.com.fiap.oficinamecanicaapi.controller.auth.AuthController;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthenticationUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecaInsumo.PecaInsumoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.JwtTokenUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.infrastructure.config.security.SecurityConfig;
import br.com.fiap.oficinamecanicaapi.infrastructure.config.security.RotasPublicas;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Contrato da documentação OpenAPI gerada pelo springdoc a partir dos controllers reais.
 * Protege as respostas comuns aplicadas pelo {@link OpenApiConfig}: as interfaces *API declaram só as específicas.
 */
@WebMvcTest(controllers = {ClienteController.class, VeiculoController.class, PecaInsumoController.class,
        ServicoController.class, AuthController.class})
@Import({SecurityConfig.class, OpenApiConfig.class})
@ImportAutoConfiguration({SpringDocConfiguration.class, SpringDocConfigProperties.class,
        SpringDocWebMvcConfiguration.class})
class OpenApiConfigTest {

    private static final String API_DOCS = "/api-docs-personalized";
    private static final String OPERACOES = "$.paths.*.*";
    /**
     * CRUDs de cliente, veículo, insumo e serviço (5 operações cada) na data do teste.
     */
    private static final int OPERACOES_PROTEGIDAS_MINIMO = 20;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteUseCasePortIn clienteUseCase;

    @MockitoBean
    private VeiculoUseCasePortIn veiculoUseCase;

    @MockitoBean
    private PecaInsumoUseCasePortIn pecaInsumoUseCase;

    @MockitoBean
    private ServicoUseCasePortIn servicoUseCase;

    @MockitoBean
    private AuthenticationUseCasePortIn authenticationUseCase;

    @MockitoBean
    private JwtTokenUseCasePortIn jwtTokenProvider;

    @MockitoBean
    private UsuarioUseCasePortIn usuarioUseCase;

    private DocumentContext documentacao;

    @BeforeEach
    void carregarDocumentacao() throws Exception {
        String json = mockMvc.perform(get(API_DOCS)).andReturn().getResponse().getContentAsString();
        documentacao = JsonPath.parse(json);
    }

    @Test
    @DisplayName("Deve publicar o esquema de segurança Bearer JWT")
    void devePublicarEsquemaBearer() {
        Map<String, Object> esquema = documentacao.read("$.components.securitySchemes." + OpenApiConfig.BEARER_AUTH);

        assertThat(esquema).containsEntry("type", "http")
                .containsEntry("scheme", "bearer")
                .containsEntry("bearerFormat", "JWT");
    }

    @Test
    @DisplayName("Toda rota protegida deve exigir Bearer e documentar 401, 403 e 500")
    void rotasProtegidasDevemTerRespostasComuns() {
        Map<String, Map<String, Map<String, Object>>> paths = documentacao.read("$.paths");

        List<Map<String, Object>> protegidas = paths.entrySet().stream()
                .filter(path -> !RotasPublicas.isPublica(path.getKey()))
                .flatMap(path -> path.getValue().values().stream())
                .toList();

        assertThat(protegidas).hasSizeGreaterThanOrEqualTo(OPERACOES_PROTEGIDAS_MINIMO);
        assertThat(protegidas).allSatisfy(operacao -> {
            assertThat(respostas(operacao)).containsKeys("401", "403", "500");
            assertThat(operacao.get("security")).asString().contains(OpenApiConfig.BEARER_AUTH);
        });
    }

    @Test
    @DisplayName("Rotas públicas (RotasPublicas) não devem exigir Bearer e devem documentar 500")
    void rotasPublicasNaoDevemExigirBearer() {
        Map<String, Map<String, Map<String, Object>>> paths = documentacao.read("$.paths");

        paths.entrySet().stream()
                .filter(path -> RotasPublicas.isPublica(path.getKey()))
                .flatMap(path -> path.getValue().values().stream())
                .forEach(operacao -> {
                    assertThat(operacao).doesNotContainKey("security");
                    assertThat(respostas(operacao)).containsKey("500").doesNotContainKeys("401", "403");
                });
    }

    @Test
    @DisplayName("Não deve documentar 503 e 504, que são respostas de gateway e não da aplicação")
    void naoDeveDocumentarRespostasDeGateway() {
        List<Map<String, Object>> respostas = documentacao.read(OPERACOES + ".responses");

        assertThat(respostas)
                .isNotEmpty()
                .allSatisfy(r -> assertThat(r).doesNotContainKeys("503", "504"));
    }

    @Test
    @DisplayName("Deve manter as respostas específicas declaradas na interface *API")
    void deveManterRespostasEspecificas() {
        assertThat(respostas(documentacao.read("$.paths['/clientes'].post")))
                .containsKeys("201", "400", "409");
        assertThat(respostas(documentacao.read("$.paths['/veiculos'].post")))
                .containsKeys("201", "400", "404", "409");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> respostas(Map<String, Object> operacao) {
        return (Map<String, Object>) operacao.get("responses");
    }
}
