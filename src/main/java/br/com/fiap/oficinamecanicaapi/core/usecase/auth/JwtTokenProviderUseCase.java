package br.com.fiap.oficinamecanicaapi.core.usecase.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.JwtTokenUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Set;

@Service
public class JwtTokenProviderUseCase implements JwtTokenUseCasePortIn {

    @Value("${api.secret}")
    private String SECRET;

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProviderUseCase.class);

    @Value("${api.jwt.expiration-minutes:60}")
    private long expiracaoMinutos = 60;

    private final MeterRegistry meterRegistry;

    public JwtTokenProviderUseCase(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String gerarToken(DetalhesUsuario detalhesUsuario) throws ApplicationException {
        try {
            Date now = new Date();
            Date expirationDate = new Date(now.getTime() + Duration.ofMinutes(expiracaoMinutos).toMillis());

            Set<String> permissoes = detalhesUsuario.permissoes() != null
                    ? detalhesUsuario.permissoes()
                    : Set.of();

            String token = Jwts.builder()
                    .claim("username", detalhesUsuario.usuario().nomeUsuario())
                    .claim("name", detalhesUsuario.usuario().nome())
                    .claim("email", detalhesUsuario.usuario().email())
                    .claim("role", detalhesUsuario.usuario().perfil().name())
                    .claim("permissions", permissoes)
                    .issuedAt(now)
                    .expiration(expirationDate)
                    .signWith(Keys.hmacShaKeyFor(getSigningKey()), SignatureAlgorithm.HS512)
                    .compact();

            log.debug("action=jwt_emitido username={} perfil={} expiraEm={}",
                    detalhesUsuario.usuario().nomeUsuario(), detalhesUsuario.usuario().perfil(),
                    expirationDate.toInstant());
            return token;

        } catch (Exception e) {
            log.error("action=jwt_erro_geracao username={}", detalhesUsuario.usuario().nomeUsuario(), e);
            throw new ApplicationException(500, "Erro ao gerar o token");
        }
    }

    @Override
    public Boolean validarToken(String token) throws ApplicationException {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(getSigningKey()))
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("action=jwt_expirado expirouEm={}", ex.getClaims().getExpiration().toInstant());
            contarRejeitado("expirado");
            throw new ApplicationException(401, "Token expirado. Faça login novamente.");
        } catch (Exception ex) {
            log.warn("action=jwt_invalido motivo={}", ex.getClass().getSimpleName());
            contarRejeitado("invalido");
            throw new ApplicationException(401, "Token inválido");
        }
    }

    @Override
    public String extrairNomeUsuario(String token) throws ApplicationException {
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(getSigningKey()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("username", String.class);
        } catch (Exception ex) {
            log.warn("Error extracting username from token: {}", ex.getMessage());
            throw new ApplicationException(401, "Token inválido");
        }
    }

    private void contarRejeitado(String motivo) {
        meterRegistry.counter("oficina.auth.token.rejeitado", "motivo", motivo).increment();
    }

    private byte[] getSigningKey() {
        return SECRET.getBytes(StandardCharsets.UTF_8);
    }
}