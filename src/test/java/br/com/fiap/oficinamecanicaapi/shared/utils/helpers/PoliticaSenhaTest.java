package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaSenhaTest {

    private final PoliticaSenha politica = new PoliticaSenha();

    @ParameterizedTest
    @ValueSource(strings = {"Admin@123", "Oficina@2026", "aB3!aB3!", "Senha-Forte_9"})
    @DisplayName("Deve aceitar senha com 8 a 64 caracteres, maiúscula, minúscula, número e especial permitido")
    void deveAceitarSenhaForte(String senha) {
        assertThatCode(() -> politica.validar(senha)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve aceitar exatamente 64 caracteres")
    void deveAceitarLimiteMaximo() {
        String senha = "Aa1!" + "a".repeat(60);

        assertThatCode(() -> politica.validar(senha)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "Ab1!",           // curta demais
            "admin@123",      // sem maiúscula
            "ADMIN@123",      // sem minúscula
            "Admin@abc",      // sem número
            "Admin1234",      // sem especial
            "Admin;1234",     // especial proibido (;)
            "Admin'1234",     // especial proibido (')
            "Admin 1234!"})   // espaço não é permitido
    @DisplayName("Deve recusar com 400 senha ausente ou fora da política")
    void deveRecusarSenhaFraca(String senha) {
        assertThatThrownBy(() -> politica.validar(senha))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(400);
    }

    @Test
    @DisplayName("Deve recusar senha com mais de 64 caracteres")
    void deveRecusarSenhaLongaDemais() {
        String senha = "Aa1!" + "a".repeat(61);

        assertThatThrownBy(() -> politica.validar(senha))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(400);
    }

    @Test
    @DisplayName("Deve expor os caracteres especiais permitidos")
    void deveExporEspeciaisPermitidos() {
        assertThat(politica.allowedSpecials()).isEqualTo(PoliticaSenha.ALLOWED_SPECIALS).doesNotContain(";", "'", "\"");
    }
}
