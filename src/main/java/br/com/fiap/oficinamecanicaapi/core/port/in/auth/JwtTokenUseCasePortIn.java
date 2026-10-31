package br.com.fiap.oficinamecanicaapi.core.port.in.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;

public interface JwtTokenUseCasePortIn {

    String gerarToken(DetalhesUsuario detalhesUsuario);

    Boolean validarToken(String token);

    String extrairNomeUsuario(String token);

}
