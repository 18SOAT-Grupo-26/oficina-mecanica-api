package br.com.fiap.oficinamecanicaapi.core.domain.usuario.model;

import java.util.Set;

public record DetalhesUsuario(
        Usuario usuario,
        Set<String> permissoes
) {
}
