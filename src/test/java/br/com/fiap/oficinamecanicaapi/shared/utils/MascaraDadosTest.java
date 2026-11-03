package br.com.fiap.oficinamecanicaapi.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MascaraDadosTest {

    @ParameterizedTest
    @CsvSource({
            "ana@oficina.com.br, a***@oficina.com.br",
            "' bruno@oficina.com.br ', b***@oficina.com.br",
            "admin, ad***",
            "a, a***",
            "x@y.com, x***@y.com"
    })
    @DisplayName("Deve mascarar e-mail mantendo a 1ª letra e o domínio, e nome de usuário mantendo até 2 letras")
    void deveMascararIdentificador(String identificador, String esperado) {
        assertThat(MascaraDados.mascararIdentificador(identificador)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("Deve devolver só a máscara quando o identificador estiver ausente")
    void deveMascararIdentificadorAusente(String identificador) {
        assertThat(MascaraDados.mascararIdentificador(identificador)).isEqualTo("***");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@oficina.com.br", "admin"})
    @DisplayName("Nunca deve devolver o identificador completo")
    void naoDeveExporIdentificador(String identificador) {
        assertThat(MascaraDados.mascararIdentificador(identificador)).isNotEqualTo(identificador);
    }
}
