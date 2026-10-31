package br.com.fiap.oficinamecanicaapi.core.domain.shared.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;

import java.math.BigDecimal;

/**
 * Regras comuns aos itens do catálogo da oficina (serviço e peça ou insumo): nome, preço e descrição.
 * Cada método valida e devolve o valor já no formato guardado.
 */
public final class RegrasCatalogo {

    static final int NOME_TAMANHO_MINIMO = 3;
    static final int DESCRICAO_TAMANHO_MAXIMO = 500;
    static final int PRECO_CASAS_DECIMAIS = 2;

    private RegrasCatalogo() {
    }

    /**
     * Nome obrigatório, com pelo menos 3 caracteres; devolve sem espaços nas pontas.
     */
    public static String nome(String nome) {
        String aparado = nome == null ? null : nome.trim();
        if (aparado == null || aparado.isEmpty()) {
            throw new DadoInvalidoException("O campo nome é obrigatório");
        }
        if (aparado.length() < NOME_TAMANHO_MINIMO) {
            throw new DadoInvalidoException("O nome deve ter no mínimo 3 caracteres");
        }
        return aparado;
    }

    /**
     * Preço obrigatório, positivo e com no máximo 2 casas decimais; devolve sempre com 2 casas.
     */
    public static BigDecimal preco(BigDecimal preco) {
        if (preco == null) {
            throw new DadoInvalidoException("O campo preco é obrigatório");
        }
        if (preco.signum() <= 0 || preco.stripTrailingZeros().scale() > PRECO_CASAS_DECIMAIS) {
            throw new DadoInvalidoException("Preço inválido");
        }
        return preco.setScale(PRECO_CASAS_DECIMAIS);
    }

    /**
     * Descrição opcional, com no máximo 500 caracteres.
     */
    public static String descricao(String descricao) {
        if (descricao != null && descricao.length() > DESCRICAO_TAMANHO_MAXIMO) {
            throw new DadoInvalidoException("A descrição deve ter no máximo 500 caracteres");
        }
        return descricao;
    }
}
