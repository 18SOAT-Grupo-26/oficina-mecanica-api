package br.com.fiap.oficinamecanicaapi.core.port.in.identity;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.AddressEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Address;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface AddressUseCasePortIn {

    AddressEntity registerAddress(@NotNull @Valid Address address);

}
