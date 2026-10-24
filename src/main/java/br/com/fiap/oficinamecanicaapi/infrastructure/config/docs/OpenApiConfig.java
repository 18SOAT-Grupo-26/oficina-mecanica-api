package br.com.fiap.oficinamecanicaapi.infrastructure.config.docs;

import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TreeMap;

/**
 * Documentação OpenAPI. As respostas comuns a todas as operações (401/403 nas rotas protegidas e 500 em todas)
 * e o esquema de segurança JWT são aplicados aqui; cada interface {@code *API} declara só as respostas
 * específicas do endpoint.
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER_AUTH = "bearerAuth";
    private static final String ROTAS_PUBLICAS = "/auth/";
    private static final String ERROR_TEMPLATE_REF = "#/components/schemas/" + ErrorTemplate.class.getSimpleName();

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Oficina Mecânica v1")
                        .version("1.0")
                        .description("Documentação da API da Oficina Mecânica"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtido em POST /auth/login")));
    }

    @Bean
    public OpenApiCustomizer respostasComunsCustomizer() {
        return openApi -> {
            registrarErrorTemplate(openApi);
            openApi.getPaths().forEach((path, item) -> item.readOperations().forEach(operacao -> {
                ApiResponses respostas = operacao.getResponses();
                if (!path.startsWith(ROTAS_PUBLICAS)) {
                    operacao.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                    respostas.putIfAbsent("401", new ApiResponse().description("Token ausente ou inválido"));
                    respostas.putIfAbsent("403", respostaDeErro("Usuário sem perfil autorizado para esta operação"));
                }
                respostas.putIfAbsent("500", respostaDeErro("Erro interno inesperado"));
                operacao.setResponses(ordenadas(respostas));
            }));
        };
    }

    /** Garante o schema do corpo de erro mesmo que nenhuma {@code *API} o referencie diretamente. */
    private static void registrarErrorTemplate(OpenAPI openApi) {
        Components components = openApi.getComponents();
        ModelConverters.getInstance().readAll(ErrorTemplate.class)
                .forEach((nome, schema) -> {
                    if (components.getSchemas() == null || !components.getSchemas().containsKey(nome)) {
                        components.addSchemas(nome, schema);
                    }
                });
    }

    private static ApiResponse respostaDeErro(String descricao) {
        return new ApiResponse()
                .description(descricao)
                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                        new MediaType().schema(new Schema<>().$ref(ERROR_TEMPLATE_REF))));
    }

    /** Ordena por código para o Swagger exibir 2xx, 4xx e 5xx em sequência. */
    private static ApiResponses ordenadas(ApiResponses respostas) {
        ApiResponses ordenadas = new ApiResponses();
        new TreeMap<String, ApiResponse>(respostas).forEach(ordenadas::addApiResponse);
        if (respostas.getExtensions() != null) {
            respostas.getExtensions().forEach(ordenadas::addExtension);
        }
        return ordenadas;
    }
}
