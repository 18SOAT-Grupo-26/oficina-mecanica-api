package br.com.fiap.oficinamecanicaapi.core.port.in.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.RegisterRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface AuthenticationUseCasePortIn {

    void registerUser(RegisterRequest registerRequest) throws ApplicationException;

    AuthToken authenticate(SigninRequest request) throws ApplicationException;

    Boolean validateJwtToken(String token) throws ApplicationException;

}
