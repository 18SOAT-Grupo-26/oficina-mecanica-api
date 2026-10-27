package br.com.fiap.oficinamecanicaapi.core.usecase.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.JwtTokenUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

@Service
public class JwtTokenProviderUseCase implements JwtTokenUseCasePortIn {

    @Value("${api.secret}")
    private String SECRET;

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProviderUseCase.class);
    private static final long EXPIRATION_TIME = 60 * 60 * 1000;

    @Override
    public String gerarToken(DetalhesUsuario detalhesUsuario) throws ApplicationException {
        try {
            Date now = new Date();
            Date expirationDate = new Date(now.getTime() + EXPIRATION_TIME);

            Set<String> permissoes = detalhesUsuario.permissoes() != null
                    ? detalhesUsuario.permissoes()
                    : Set.of();

            return Jwts.builder()
                    .claim("username", detalhesUsuario.usuario().nomeUsuario())
                    .claim("name", detalhesUsuario.usuario().nome())
                    .claim("email", detalhesUsuario.usuario().email())
                    .claim("role", detalhesUsuario.usuario().perfil().name())
                    .claim("permissions", permissoes)
                    .issuedAt(now)
                    .expiration(expirationDate)
                    .signWith(Keys.hmacShaKeyFor(getSigningKey()), SignatureAlgorithm.HS512)
                    .compact();

        } catch (Exception e) {
            log.error("Error generating JWT token", e);
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
            log.warn("Token expired: {}", ex.getMessage());
            throw new ApplicationException(401, "Token expirado. Faça login novamente.");
        } catch (Exception ex) {
            log.warn("Invalid token: {}", ex.getMessage());
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

    private byte[] getSigningKey() {
        return SECRET.getBytes(StandardCharsets.UTF_8);
    }
}