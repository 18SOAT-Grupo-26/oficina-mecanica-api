package br.com.fiap.oficinamecanicaapi.core.domain.auth.model;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Identity;
import br.com.fiap.oficinamecanicaapi.core.domain.user.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotNull @Valid Identity identity,
        @NotNull @Valid User user
) {
}
