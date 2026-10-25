package br.com.fiap.oficinamecanicaapi.core.port.in.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.CadastroRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface AuthenticationUseCasePortIn {

    void cadastrarUsuario(CadastroRequest request) throws ApplicationException;

    AuthToken autenticar(SigninRequest request) throws ApplicationException;

    Boolean validarToken(String token) throws ApplicationException;

}
