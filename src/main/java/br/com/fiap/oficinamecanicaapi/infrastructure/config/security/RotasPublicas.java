package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Único lugar que libera rota sem JWT. Tudo o que não estiver aqui exige autenticação
 * ({@link SecurityConfig}) e é documentado com bearerAuth no Swagger (OpenApiConfig).
 * Incluir uma rota nesta classe é uma decisão de segurança: revise no PR.
 */
public final class RotasPublicas {

    /**
     * Login e validação do token. Listadas uma a uma, sem curinga: uma rota nova em /auth nasce protegida
     * (o cadastro de usuários, por exemplo, exige ADMINISTRATOR).
     */
    protected static final String[] AUTENTICACAO = {"/auth/login", "/auth/validate"};

    public static final String[] DOCUMENTACAO = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/api-docs-personalized/**",
            "/documentation/**"
    };

    /**
     * Expostos só health e prometheus (management.endpoints.web.exposure).
     */
    public static final String[] MONITORAMENTO = {"/actuator/**"};

    private static final List<PathPattern> PADROES = Arrays.stream(todas())
            .map(PathPatternParser.defaultInstance::parse)
            .toList();

    private RotasPublicas() {
    }

    public static String[] todas() {
        return Stream.of(AUTENTICACAO, DOCUMENTACAO, MONITORAMENTO)
                .flatMap(Arrays::stream)
                .toArray(String[]::new);
    }

    /**
     * Recebe o caminho sem o context-path (ex.: {@code /auth/login}).
     */
    public static boolean isPublica(String path) {
        PathContainer caminho = PathContainer.parsePath(path);
        return PADROES.stream().anyMatch(padrao -> padrao.matches(caminho));
    }
}
