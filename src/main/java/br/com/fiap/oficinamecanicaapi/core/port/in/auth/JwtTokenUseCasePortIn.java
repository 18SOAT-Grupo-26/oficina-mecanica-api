package br.com.fiap.oficinamecanicaapi.core.port.in.auth;


import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface JwtTokenUseCasePortIn {

    String gerarToken(DetalhesUsuario detalhesUsuario) throws ApplicationException;
    Boolean validarToken(String token) throws ApplicationException;
    String extrairNomeUsuario(String token) throws ApplicationException;

}
