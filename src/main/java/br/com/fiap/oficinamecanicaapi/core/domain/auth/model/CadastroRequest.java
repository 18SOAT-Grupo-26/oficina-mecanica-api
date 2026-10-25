package br.com.fiap.oficinamecanicaapi.core.domain.auth.model;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Identidade;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CadastroRequest(
        @NotNull @Valid Identidade identidade,
        @NotNull @Valid Usuario usuario
) {
}
