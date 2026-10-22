package br.com.fiap.oficinamecanicaapi.core.domain.shared.model;

import java.util.List;
import java.util.function.Function;

/** Resultado paginado do domínio, sem depender do Spring Data. */
public record Pagina<T>(
        List<T> itens,
        int pagina,
        int tamanho,
        long total
) {

    public <R> Pagina<R> map(Function<T, R> conversor) {
        return new Pagina<>(itens.stream().map(conversor).toList(), pagina, tamanho, total);
    }
}
