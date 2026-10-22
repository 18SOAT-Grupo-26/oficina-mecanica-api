package br.com.fiap.oficinamecanicaapi.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjValidatorTest {

    private static final String CNPJ_NUMERICO = "11222333000181";
    private static final String CNPJ_NUMERICO_COM_MASCARA = "11.222.333/0001-81";
    private static final String CNPJ_ALFANUMERICO = "12ABC34501DE35";
    private static final String CNPJ_ALFANUMERICO_COM_MASCARA = "12.ABC.345/01DE-35";

    @ParameterizedTest
    @ValueSource(strings = {CNPJ_NUMERICO, CNPJ_NUMERICO_COM_MASCARA, CNPJ_ALFANUMERICO,
            CNPJ_ALFANUMERICO_COM_MASCARA, "12.abc.345/01de-35"})
    @DisplayName("Deve aceitar CNPJ numérico e alfanumérico, com ou sem máscara")
    void deveAceitarCnpjValido(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "11222333000182",   // segundo dígito verificador errado
            "11222333000191",   // primeiro dígito verificador errado
            "12ABC34501DE36",   // alfanumérico com dígito verificador errado
            "12ABC34501DEA5",   // dígito verificador com letra
            "00000000000000",   // caracteres repetidos
            "1122233300018",    // tamanho menor
            "112223330001811",  // tamanho maior
            ""
    })
    @DisplayName("Deve rejeitar CNPJ com dígito verificador, formato ou tamanho inválido")
    void deveRejeitarCnpjInvalido(String cnpj) {
        assertThat(CnpjValidator.isValid(cnpj)).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar CNPJ nulo")
    void deveRejeitarCnpjNulo() {
        assertThat(CnpjValidator.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("Deve normalizar removendo a máscara e convertendo para maiúsculas")
    void deveNormalizar() {
        assertThat(CnpjValidator.normalize("12.abc.345/01de-35")).isEqualTo(CNPJ_ALFANUMERICO);
    }

    @Test
    @DisplayName("Deve formatar o CNPJ alfanumérico com a máscara oficial")
    void deveFormatar() {
        assertThat(CnpjValidator.format(CNPJ_ALFANUMERICO)).isEqualTo(CNPJ_ALFANUMERICO_COM_MASCARA);
    }
}
