package br.com.fiap.oficinamecanicaapi.core.port.in.identity;


import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.IdentityEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Identity;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface IdentityUseCasePortIn {

    IdentityEntity registerIdentity(Identity identityRequest) throws ApplicationException;

}