package br.com.fiap.oficinamecanicaapi.core.port.in.identity;


import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.ContactEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Contact;

public interface ContactUseCasePortIn {

    ContactEntity registerContact(Contact contact);

}
