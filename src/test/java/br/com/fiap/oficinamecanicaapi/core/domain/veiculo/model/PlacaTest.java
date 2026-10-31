package br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlacaTest {

    private static final String PLACA_ANTIGA = "ABC1234";
    private static final String PLACA_MERCOSUL = "BRA2E19";
    private static final String MENSAGEM_INVALIDA = "Placa inválida — use o formato AAA9999 ou AAA9A99";

    @ParameterizedTest
    @CsvSource({
            "ABC1234, ABC1234",
            "abc-1234, ABC1234",
            "ABC 1234, ABC1234",
            "BRA2E19, BRA2E19",
            "bra2e19, BRA2E19"
    })
    @DisplayName("Deve aceitar placa antiga e Mercosul, normalizando hífen, espaço e minúsculas")
    void deveAceitarPlacaValida(String entrada, String esperado) {
        assertThat(new Placa(entrada).valor()).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "AB12345",   // só duas letras
            "ABCD123",   // quatro letras
            "ABC12345",  // tamanho maior
            "ABC123",    // tamanho menor
            "1BC1234",   // começa com dígito
            "ABC1E1A",   // termina com letra
            "   "
    })
    @DisplayName("Deve rejeitar placa fora dos formatos antigo e Mercosul com erro 400")
    void deveRejeitarPlacaInvalida(String valor) {
        DadoInvalidoException exception = assertThrows(DadoInvalidoException.class, () -> new Placa(valor));

        assertThat(exception.getMessage()).isEqualTo(MENSAGEM_INVALIDA);
    }

    @Test
    @DisplayName("Placas com o mesmo valor normalizado devem ser iguais")
    void deveSerIgualPorValor() {
        assertThat(new Placa("abc-1234")).isEqualTo(new Placa(PLACA_ANTIGA));
        assertThat(new Placa(PLACA_MERCOSUL)).isNotEqualTo(new Placa(PLACA_ANTIGA));
    }
}
