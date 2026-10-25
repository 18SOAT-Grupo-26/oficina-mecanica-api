package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Objeto de valor de um valor em reais. Guarda sempre duas casas decimais (HALF_EVEN), então a
 * igualdade é por valor: {@code 10.0} e {@code 10.00} são o mesmo valor.
 */
public record ValorMonetario(BigDecimal valor) {

    public static final ValorMonetario ZERO = new ValorMonetario(BigDecimal.ZERO);

    private static final int CASAS_DECIMAIS = 2;

    public ValorMonetario {
        if (valor == null) {
            throw new ApplicationException(400, "O valor monetário é obrigatório");
        }
        valor = valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_EVEN);
        if (valor.signum() < 0) {
            throw new ApplicationException(400, "O valor monetário não pode ser negativo");
        }
    }

    public static ValorMonetario de(String valor) {
        return new ValorMonetario(new BigDecimal(valor));
    }

    public ValorMonetario somar(ValorMonetario outro) {
        return new ValorMonetario(valor.add(outro.valor));
    }

    public ValorMonetario multiplicar(int quantidade) {
        return new ValorMonetario(valor.multiply(BigDecimal.valueOf(quantidade)));
    }
}
