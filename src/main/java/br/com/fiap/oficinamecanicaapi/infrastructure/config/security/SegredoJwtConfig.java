package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SegredoJwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Set;

/**
 * Resolve a chave do JWT (API_SECRET) na subida, para a API falhar cedo em vez de responder 500 no primeiro login.
 * <ul>
 *   <li>vazia: gera uma chave aleatória (só para ambiente local; os tokens deixam de valer a cada reinício);</li>
 *   <li>com menos de 64 bytes (mínimo do HS512) ou publicada no histórico do repositório: a API não sobe.</li>
 * </ul>
 */
@Configuration
public class SegredoJwtConfig {

    private static final Logger log = LoggerFactory.getLogger(SegredoJwtConfig.class);

    static final int TAMANHO_MINIMO_BYTES = 64;

    /**
     * SHA-256 das chaves que já estiveram versionadas (A, B e C no relatório de vulnerabilidades). São públicas:
     * aceitá-las permitiria a qualquer um forjar tokens. Guardamos só o hash, nunca a chave.
     */
    static final Set<String> HASHES_CHAVES_PUBLICADAS = Set.of(
            "e1f7736e57d6f4cee8738596b5392435f7a07f11f6900a54dadf8899f2e062be",
            "ab46033ab5b31fac379121f4a45cf92a4d6598440b9ed812237c55b1f270be75",
            "9cad21d5257bd93b6ac69b56f53a108149e0bde8500054aa5ee5529c16e715f5");

    private static final SecureRandom GERADOR = new SecureRandom();

    @Bean
    public SegredoJwt segredoJwt(@Value("${api.secret:}") String configurado) {
        return resolver(configurado, HASHES_CHAVES_PUBLICADAS);
    }

    static SegredoJwt resolver(String configurado, Set<String> hashesPublicados) {
        if (configurado == null || configurado.isBlank()) {
            log.warn("action=jwt_chave_aleatoria API_SECRET vazio: gerada uma chave aleatória; os tokens deixam de "
                    + "valer a cada reinício. Fora do ambiente local, defina API_SECRET (openssl rand -base64 64)");
            byte[] chave = new byte[TAMANHO_MINIMO_BYTES];
            GERADOR.nextBytes(chave);
            return new SegredoJwt(chave);
        }
        byte[] chave = configurado.getBytes(StandardCharsets.UTF_8);
        if (chave.length < TAMANHO_MINIMO_BYTES) {
            throw new IllegalStateException("API_SECRET precisa ter no mínimo " + TAMANHO_MINIMO_BYTES
                    + " bytes (HS512); gere com: openssl rand -base64 64");
        }
        if (hashesPublicados.contains(sha256(chave))) {
            throw new IllegalStateException("API_SECRET é uma chave publicada no histórico do repositório; "
                    + "gere outra (openssl rand -base64 64) ou deixe vazio no ambiente local");
        }
        return new SegredoJwt(chave);
    }

    private static String sha256(byte[] valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
