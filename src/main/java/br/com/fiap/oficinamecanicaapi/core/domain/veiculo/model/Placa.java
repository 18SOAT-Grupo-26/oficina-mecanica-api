package br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;

/**
 * Objeto de valor da placa do veículo.
 *
 * <p>Aceita o formato antigo ({@code AAA9999}) e o Mercosul ({@code AAA9A99}), com ou sem hífen
 * e em minúsculas; guarda o valor sem separadores e em maiúsculas.
 */
public record Placa(String valor) {

    private static final String FORMATO = "[A-Z]{3}[0-9][A-Z0-9][0-9]{2}";
    private static final String MENSAGEM_INVALIDA = "Placa inválida — use o formato AAA9999 ou AAA9A99";

    public Placa {
        String normalizada = valor == null ? null : valor.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (normalizada == null || !normalizada.matches(FORMATO)) {
            throw new DadoInvalidoException(MENSAGEM_INVALIDA);
        }
        valor = normalizada;
    }
}
