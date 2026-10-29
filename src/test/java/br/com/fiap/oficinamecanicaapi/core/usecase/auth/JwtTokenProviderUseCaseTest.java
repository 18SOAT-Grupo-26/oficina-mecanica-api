package br.com.fiap.oficinamecanicaapi.core.usecase.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SegredoJwt;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderUseCaseTest {

    /** 88 bytes: o HS512 exige chave de pelo menos 64. */
    private static final String SEGREDO =
            "dGVzdGUtZGUtc2VncmVkby1jb20tbWFpcy1kZS02NC1ieXRlcy1wYXJhLW8taHM1MTItZnVuY2lvbmFyLW9rISE=";
    private static final String OUTRO_SEGREDO =
            "b3V0cm8tc2VncmVkby1jb20tbWFpcy1kZS02NC1ieXRlcy1wYXJhLW8taHM1MTItZnVuY2lvbmFyLW9rISEhISE=";
    private static final DetalhesUsuario DETALHES = new DetalhesUsuario(
            new Usuario("Ana Admin", "ana@oficina.com.br", "ana", "{bcrypt}x", false, Perfil.FUNCIONARIO),
            Set.of("OS_LER"));

    private SimpleMeterRegistry meterRegistry;
    private JwtTokenProviderUseCase provider;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        provider = comSegredo(SEGREDO);
    }

    @Test
    @DisplayName("Deve gerar token assinado com usuário, nome, e-mail, perfil, permissões e expiração de 1 hora")
    void deveGerarToken() {
        String token = provider.gerarToken(DETALHES);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertThat(claims.get("username", String.class)).isEqualTo("ana");
        assertThat(claims.get("name", String.class)).isEqualTo("Ana Admin");
        assertThat(claims.get("email", String.class)).isEqualTo("ana@oficina.com.br");
        assertThat(claims.get("role", String.class)).isEqualTo("FUNCIONARIO");
        assertThat(claims.get("permissions")).asInstanceOf(InstanceOfAssertFactories.LIST).containsExactly("OS_LER");
        assertThat(Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant()))
                .isEqualTo(Duration.ofHours(1));
    }

    @Test
    @DisplayName("Deve gerar token com permissões vazias quando o usuário não tem nenhuma")
    void deveGerarTokenSemPermissoes() {
        DetalhesUsuario semPermissoes = new DetalhesUsuario(DETALHES.usuario(), null);

        String token = provider.gerarToken(semPermissoes);

        assertThat(provider.validarToken(token)).isTrue();
    }

    @Test
    @DisplayName("Deve validar o token gerado e extrair o nome de usuário")
    void deveValidarEExtrair() {
        String token = provider.gerarToken(DETALHES);

        assertThat(provider.validarToken(token)).isTrue();
        assertThat(provider.extrairNomeUsuario(token)).isEqualTo("ana");
        assertThat(meterRegistry.find("oficina.auth.token.rejeitado").counter()).isNull();
    }

    @Test
    @DisplayName("Deve responder 401 'Token expirado' e contar motivo=expirado")
    void deveRecusarTokenExpirado() {
        Instant duasHorasAtras = Instant.now().minus(Duration.ofHours(2));
        String expirado = Jwts.builder()
                .claim("username", "ana")
                .issuedAt(Date.from(duasHorasAtras))
                .expiration(Date.from(duasHorasAtras.plus(Duration.ofHours(1))))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> provider.validarToken(expirado))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Token expirado. Faça login novamente.")
                .extracting("errorCode").isEqualTo(401);
        assertThat(rejeitados("expirado")).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve responder 401 'Token inválido' para token malformado e contar motivo=invalido")
    void deveRecusarTokenMalformado() {
        assertThatThrownBy(() -> provider.validarToken("nao-e-um-jwt"))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Token inválido")
                .extracting("errorCode").isEqualTo(401);
        assertThat(rejeitados("invalido")).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve recusar token assinado com outro segredo")
    void deveRecusarAssinaturaDeOutroSegredo() {
        String tokenDeOutroSistema = comSegredo(OUTRO_SEGREDO).gerarToken(DETALHES);

        assertThatThrownBy(() -> provider.validarToken(tokenDeOutroSistema))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Token inválido");
        assertThatThrownBy(() -> provider.extrairNomeUsuario(tokenDeOutroSistema))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(401);
    }

    @Test
    @DisplayName("Deve responder 500 quando o segredo configurado é curto demais para o HS512")
    void deveFalharComSegredoCurto() {
        JwtTokenProviderUseCase comSegredoCurto = comSegredo("curto");

        assertThatThrownBy(() -> comSegredoCurto.gerarToken(DETALHES))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Erro ao gerar o token")
                .extracting("errorCode").isEqualTo(500);
    }

    private JwtTokenProviderUseCase comSegredo(String segredo) {
        return new JwtTokenProviderUseCase(meterRegistry, new SegredoJwt(segredo.getBytes(StandardCharsets.UTF_8)));
    }

    private double rejeitados(String motivo) {
        var contador = meterRegistry.find("oficina.auth.token.rejeitado").tag("motivo", motivo).counter();
        return contador == null ? 0 : contador.count();
    }
}
