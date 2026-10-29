package br.com.fiap.oficinamecanicaapi.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidatorTest {

    @Test
    @DisplayName("Deve manter só os dígitos e preservar nulo")
    void deveExtrairDigitos() {
        assertThat(CpfValidator.digits("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CpfValidator.digits(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "111.444.777-35"})
    @DisplayName("Deve aceitar CPF com dígitos verificadores corretos, com ou sem máscara")
    void deveAceitarCpfValido(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "52998224735",   // primeiro dígito verificador errado
            "52998224726",   // segundo dígito verificador errado
            "11111111111",   // sequência repetida
            "00000000000",
            "5299822472",    // 10 dígitos
            "529982247250",  // 12 dígitos
            "abc"})
    @DisplayName("Deve recusar CPF nulo, vazio, com tamanho errado, repetido ou com dígito verificador errado")
    void deveRecusarCpfInvalido(String cpf) {
        assertThat(CpfValidator.isValid(cpf)).isFalse();
    }

    @Test
    @DisplayName("Deve formatar com a máscara de exibição e devolver o valor original quando não for CPF")
    void deveFormatar() {
        assertThat(CpfValidator.format("52998224725")).isEqualTo("529.982.247-25");
        assertThat(CpfValidator.format("123")).isEqualTo("123");
        assertThat(CpfValidator.format(null)).isNull();
    }
}
