package br.com.fiap.oficinamecanicaapi.core.domain.auth.model;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param clienteId obrigatório para o perfil CLIENTE e proibido para os outros
 */
public record CadastroRequest(
        @NotBlank String nome,
        @NotBlank String email,
        @NotBlank String nomeUsuario,
        @NotBlank String senha,
        boolean trocaSenhaObrigatoria,
        @NotNull Perfil perfil,
        Long clienteId
) {
}
