package br.com.fiap.oficinamecanicaapi.core.usecase.identity;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.AddressEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Address;
import br.com.fiap.oficinamecanicaapi.core.port.in.identity.AddressUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.identity.mapper.AddressMapper;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity.AddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AddressUseCase implements AddressUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(AddressUseCase.class);

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    public AddressUseCase(AddressRepository addressRepository, AddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.addressMapper = addressMapper;
    }

    @Override
    public AddressEntity registerAddress(Address address) {
        log.info("Starting registration for address: {}", address);
        return addressRepository.save(addressMapper.toEntity(address));
    }

}
