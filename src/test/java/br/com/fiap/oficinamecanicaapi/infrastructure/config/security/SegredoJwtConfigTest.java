package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Resolução do API_SECRET na subida. Nenhuma chave real aparece aqui: a "publicada" é gerada no teste e só o hash
 * dela vai para o conjunto de chaves recusadas.
 */
class SegredoJwtConfigTest {

    /** 64 caracteres ASCII = 64 bytes, o mínimo do HS512. */
    private static final String CHAVE_VALIDA = "k".repeat(SegredoJwtConfig.TAMANHO_MINIMO_BYTES);
    private static final String CHAVE_PUBLICADA = "p".repeat(SegredoJwtConfig.TAMANHO_MINIMO_BYTES);
    private static final Set<String> SEM_PUBLICADAS = Set.of();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("API_SECRET vazio deve gerar uma chave aleatória de 64 bytes, diferente a cada subida")
    void deveGerarChaveAleatoriaQuandoVazio(String configurado) {
        byte[] primeira = SegredoJwtConfig.resolver(configurado, SEM_PUBLICADAS).chave();
        byte[] segunda = SegredoJwtConfig.resolver(configurado, SEM_PUBLICADAS).chave();

        assertThat(primeira).hasSize(SegredoJwtConfig.TAMANHO_MINIMO_BYTES);
        assertThat(primeira).isNotEqualTo(segunda);
    }

    @Test
    @DisplayName("API_SECRET válido deve virar a chave de assinatura, em bytes UTF-8")
    void deveUsarChaveConfigurada() {
        byte[] chave = SegredoJwtConfig.resolver(CHAVE_VALIDA, SEM_PUBLICADAS).chave();

        assertThat(chave).isEqualTo(CHAVE_VALIDA.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("API_SECRET com menos de 64 bytes deve impedir a subida")
    void deveRecusarChaveCurta() {
        String curta = "k".repeat(SegredoJwtConfig.TAMANHO_MINIMO_BYTES - 1);

        assertThatThrownBy(() -> SegredoJwtConfig.resolver(curta, SEM_PUBLICADAS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no mínimo 64 bytes");
    }

    @Test
    @DisplayName("API_SECRET publicado no histórico do repositório deve impedir a subida")
    void deveRecusarChavePublicada() {
        Set<String> publicadas = Set.of(sha256(CHAVE_PUBLICADA));

        assertThatThrownBy(() -> SegredoJwtConfig.resolver(CHAVE_PUBLICADA, publicadas))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chave publicada no histórico");
    }

    @Test
    @DisplayName("O conjunto real deve ter os hashes SHA-256 das 3 chaves que já foram versionadas")
    void deveConhecerAsChavesPublicadas() {
        assertThat(SegredoJwtConfig.HASHES_CHAVES_PUBLICADAS)
                .hasSize(3)
                .allSatisfy(hash -> assertThat(hash).matches("[0-9a-f]{64}"));
    }

    @Test
    @DisplayName("O toString da chave não deve expor o conteúdo")
    void naoDeveExporChaveNoToString() {
        var segredo = SegredoJwtConfig.resolver(CHAVE_VALIDA, SEM_PUBLICADAS);

        assertThat(segredo.toString()).isEqualTo("SegredoJwt[64 bytes]").doesNotContain(CHAVE_VALIDA);
    }

    private static String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
