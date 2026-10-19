package br.com.fiap.oficinamecanicaapi.core.port.in.user;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.IdentityEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.user.model.User;
import br.com.fiap.oficinamecanicaapi.core.domain.user.model.UserDetails;

public interface UserUseCasePortIn {

    void registerUser(User userRequest, IdentityEntity identityEntity);

    UserDetails retrieveUser(SigninRequest request);

}
