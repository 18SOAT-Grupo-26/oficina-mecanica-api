package br.com.fiap.oficinamecanicaapi.core.usecase.identity;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.ContactEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Contact;
import br.com.fiap.oficinamecanicaapi.core.port.in.identity.ContactUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.identity.mapper.ContactMapper;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContactUseCase implements ContactUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(ContactUseCase.class);

    private final ContactRepository contactRepository;
    private final ContactMapper contactMapper;

    public ContactUseCase(ContactRepository contactRepository, ContactMapper contactMapper) {
        this.contactRepository = contactRepository;
        this.contactMapper = contactMapper;
    }

    @Override
    public ContactEntity registerContact(Contact contact) {
        log.info("Starting registration for contact: {}", contact);
        return contactRepository.save(contactMapper.toEntity(contact));
    }

}
