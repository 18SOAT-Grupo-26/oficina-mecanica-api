package br.com.fiap.oficinamecanicaapi.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizadorNomeTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Troca de Óleo | troca de oleo",
            "troca de oleo | troca de oleo",
            "  TROCA   DE   OLEO  | troca de oleo",
            "Alinhamento & Balanceamento | alinhamento & balanceamento",
            "Revisão Ç Ã | revisao c a",
            "ÉTICA | etica"
    })
    @DisplayName("Deve ignorar acentos, maiúsculas/minúsculas e espaços extras")
    void deveNormalizarNome(String entrada, String esperado) {
        assertThat(NormalizadorNome.normalizar(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Deve preservar nomes já normalizados")
    void devePreservarNomeNormalizado() {
        assertThat(NormalizadorNome.normalizar("troca de oleo")).isEqualTo("troca de oleo");
    }

    @Test
    @DisplayName("Deve transformar string só com espaços em vazio")
    void deveRetornarVazioParaEspacos() {
        assertThat(NormalizadorNome.normalizar("   ")).isEmpty();
    }

    @Test
    @DisplayName("Deve lançar NullPointerException para nome nulo")
    void deveFalharComNulo() {
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class,
                () -> NormalizadorNome.normalizar(null));
    }
}
