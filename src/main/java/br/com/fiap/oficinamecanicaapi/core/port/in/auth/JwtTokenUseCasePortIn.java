package br.com.fiap.oficinamecanicaapi.core.port.in.auth;


import br.com.fiap.oficinamecanicaapi.core.domain.user.model.UserDetails;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface JwtTokenUseCasePortIn {

    String generateToken(UserDetails identityDetails) throws ApplicationException;
    Boolean validateToken(String token) throws ApplicationException;
    String extractUsername(String token) throws ApplicationException;

}
