package br.com.fiap.oficinamecanicaapi.core.usecase.identity;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.IdentityEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Identity;
import br.com.fiap.oficinamecanicaapi.core.port.in.identity.AddressUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.identity.ContactUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.identity.IdentityUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.identity.mapper.IdentityMapper;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity.IdentityRepository;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.helpers.IdentityPayload;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityUseCase implements IdentityUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(IdentityUseCase.class);

    private final IdentityRepository identityRepository;
    private final AddressUseCasePortIn addressService;
    private final ContactUseCasePortIn contactService;
    private final IdentityPayload identityPayload;
    private final IdentityMapper identityMapper;

    public IdentityUseCase(IdentityRepository identityRepository,
                           AddressUseCasePortIn addressService,
                           ContactUseCasePortIn contactService,
                           IdentityPayload identityPayload,
                           IdentityMapper identityMapper,
                           MeterRegistry meterRegistry) {
        this.identityRepository = identityRepository;
        this.addressService = addressService;
        this.contactService = contactService;
        this.identityPayload = identityPayload;
        this.identityMapper = identityMapper;

    }

    @Override
    @Transactional
    public IdentityEntity registerIdentity(Identity identityRequest) throws ApplicationException {
        log.info("Validating payloads: {}", identityRequest);
        validatePayloads(identityRequest);

        log.info("Starting register on database: {}", identityRequest);
        return saveIdentity(identityRequest);
    }

    private IdentityEntity saveIdentity(Identity identityRequest) {
        var contact = contactService.registerContact(identityRequest.contact());

        var address = addressService.registerAddress(identityRequest.address());

        return identityRepository.save(
                        identityMapper.persistIdentityWithContactAndAddressEntities(
                                identityRequest, contact, address));
    }

    private void validatePayloads(Identity identityRequest) {
        log.info("Validating identity payload: {}", identityRequest);
        identityPayload.validateIdentityPayload(identityRequest);
        log.info("Validating identity uniqueness: {}", identityRequest);
        identityPayload.validateIdentityUniqueness(identityRequest);
    }
}
