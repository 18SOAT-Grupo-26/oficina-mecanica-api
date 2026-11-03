package br.com.fiap.oficinamecanicaapi.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.function.Function;

/**
 * 201 com o header {@code Location} apontando para o recurso criado (ex.: {@code /api/v1/clientes/7}).
 */
final class Criado {

    private Criado() {
    }

    static <T> ResponseEntity<T> em(T recurso, Function<T, Long> id) {
        var uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id.apply(recurso))
                .toUri();
        return ResponseEntity.created(uri).body(recurso);
    }
}
