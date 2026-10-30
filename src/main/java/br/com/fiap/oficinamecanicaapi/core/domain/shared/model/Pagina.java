package br.com.fiap.oficinamecanicaapi.core.domain.shared.model;

import java.util.List;
import java.util.function.Function;

/**
 * Resultado paginado do domínio, sem depender do Spring Data. A primeira página é a 1; os adapters
 * convertem para o índice do Spring Data, que começa em 0.
 */
public record Pagina<T>(
        List<T> itens,
        int pagina,
        int tamanho,
        long total
) {

    public static final int PAGINA_PADRAO = 1;
    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    public <R> Pagina<R> map(Function<T, R> conversor) {
        return new Pagina<>(itens.stream().map(conversor).toList(), pagina, tamanho, total);
    }

    /**
     * Página ausente, zero ou negativa vira a primeira.
     */
    public static int paginaValida(Integer pagina) {
        return pagina == null || pagina < 1 ? PAGINA_PADRAO : pagina;
    }

    /**
     * Tamanho ausente ou menor que 1 usa o padrão; acima do máximo é limitado.
     */
    public static int tamanhoValido(Integer tamanho) {
        return tamanho == null || tamanho < 1 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);
    }
}
