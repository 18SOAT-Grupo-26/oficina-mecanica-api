package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValorMonetarioTest {

    @Test
    @DisplayName("Deve guardar o valor com duas casas decimais")
    void deveGuardarComDuasCasas() {
        assertThat(ValorMonetario.de("10").valor()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Deve arredondar para duas casas com HALF_EVEN")
    void deveArredondarHalfEven() {
        assertThat(ValorMonetario.de("10.125").valor()).isEqualTo(new BigDecimal("10.12"));
        assertThat(ValorMonetario.de("10.135").valor()).isEqualTo(new BigDecimal("10.14"));
    }

    @Test
    @DisplayName("Deve comparar por valor, independentemente da escala informada")
    void deveCompararPorValor() {
        assertThat(ValorMonetario.de("10.0")).isEqualTo(ValorMonetario.de("10.00"));
        assertThat(ValorMonetario.de("10.0")).hasSameHashCodeAs(ValorMonetario.de("10.00"));
    }

    @Test
    @DisplayName("Deve rejeitar valor nulo com erro 400")
    void deveRejeitarValorNulo() {
        DadoInvalidoException exception = assertThrows(DadoInvalidoException.class, () -> new ValorMonetario(null));

        assertThat(exception.getMessage()).isEqualTo("O valor monetário é obrigatório");
    }

    @Test
    @DisplayName("Deve rejeitar valor negativo com erro 400")
    void deveRejeitarValorNegativo() {
        DadoInvalidoException exception = assertThrows(DadoInvalidoException.class, () -> ValorMonetario.de("-0.01"));

        assertThat(exception.getMessage()).isEqualTo("O valor monetário não pode ser negativo");
    }

    @Test
    @DisplayName("Deve somar dois valores")
    void deveSomar() {
        assertThat(ValorMonetario.de("100.50").somar(ValorMonetario.de("59.50"))).isEqualTo(ValorMonetario.de("160"));
    }

    @Test
    @DisplayName("Deve multiplicar pela quantidade")
    void deveMultiplicarPelaQuantidade() {
        assertThat(ValorMonetario.de("30.25").multiplicar(3)).isEqualTo(ValorMonetario.de("90.75"));
    }

    @Test
    @DisplayName("Deve ter o zero como valor neutro da soma")
    void deveTerZero() {
        assertThat(ValorMonetario.ZERO.somar(ValorMonetario.de("5"))).isEqualTo(ValorMonetario.de("5"));
    }
}
