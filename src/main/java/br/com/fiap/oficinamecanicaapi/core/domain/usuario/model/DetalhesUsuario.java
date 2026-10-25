package br.com.fiap.oficinamecanicaapi.core.domain.usuario.model;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Identidade;

import java.util.Set;

public record DetalhesUsuario(
        Identidade identidade,
        Usuario usuario,
        Set<String> permissoes
) {
}