package br.com.fiap.oficinamecanicaapi.core.domain.shared.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.CnpjValidator;
import br.com.fiap.oficinamecanicaapi.shared.utils.CpfValidator;

/**
 * Objeto de valor do documento do cliente (CPF ou CNPJ).
 *
 * <p>Aceita o valor com ou sem máscara e guarda só letras e dígitos, em maiúsculas.
 * O tipo é deduzido pelo tamanho: 11 caracteres = CPF, 14 = CNPJ.
 */
public record Documento(String valor) {

    private static final int TAMANHO_CPF = 11;
    private static final int TAMANHO_CNPJ = 14;
    private static final String MENSAGEM_INVALIDO = "Documento inválido — informe um CPF ou CNPJ válido";

    public Documento {
        String normalizado = CnpjValidator.normalize(valor);
        if (!isValido(normalizado)) {
            throw new ApplicationException(400, MENSAGEM_INVALIDO);
        }
        valor = normalizado;
    }

    public TipoDocumento tipo() {
        return valor.length() == TAMANHO_CPF ? TipoDocumento.CPF : TipoDocumento.CNPJ;
    }

    private static boolean isValido(String normalizado) {
        if (normalizado == null) return false;
        return switch (normalizado.length()) {
            case TAMANHO_CPF ->
                    CpfValidator.isValid(normalizado) && normalizado.equals(CpfValidator.digits(normalizado));
            case TAMANHO_CNPJ -> CnpjValidator.isValid(normalizado);
            default -> false;
        };
    }
}
