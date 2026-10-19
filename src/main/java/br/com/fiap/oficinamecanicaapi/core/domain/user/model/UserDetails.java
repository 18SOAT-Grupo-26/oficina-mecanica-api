package br.com.fiap.oficinamecanicaapi.core.domain.user.model;


import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Identity;

import java.util.Set;

public record UserDetails(
        Identity identity,
        User user,
        Set<String> permissions
) {
}