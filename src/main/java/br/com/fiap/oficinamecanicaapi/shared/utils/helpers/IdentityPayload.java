package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Identity;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity.ContactRepository;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity.IdentityRepository;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IdentityPayload {

    private final Logger log = LoggerFactory.getLogger(IdentityPayload.class);

    private final IdentityRepository identityRepository;
    private final ContactRepository contactRepository;

    public IdentityPayload(IdentityRepository identityRepository, ContactRepository contactRepository) {
        this.identityRepository = identityRepository;
        this.contactRepository = contactRepository;
    }

    public final void validateIdentityPayload(Identity identityRequest) throws ApplicationException {
        if (identityRequest.contact() == null || identityRequest.contact().email() == null) {
            throw new ApplicationException(400, "Os dados de contato são obrigatórios");
        }
        if (!isValidEmail(identityRequest.contact().email())) {
            throw new ApplicationException(400, "E-mail inválido — deve conter '@' e um domínio válido");
        }
        if (identityRequest.fullName() == null || identityRequest.fullName().isBlank()) {
            throw new ApplicationException(400, "O nome completo é obrigatório");
        }
        if (identityRequest.givenName() == null || identityRequest.givenName().isBlank()) {
            throw new ApplicationException(400, "O nome de exibição é obrigatório");
        }
        // Documento: CPF or CNPJ, with or without mask; Documento normalizes it
        // and verifies the check digits, throwing 400 when invalid.
        if (identityRequest.documento() == null || identityRequest.documento().isBlank()) {
            throw new ApplicationException(400, "O documento (CPF ou CNPJ) é obrigatório");
        }
        new Documento(identityRequest.documento());

        if (identityRequest.contact().phoneNumber() == null
                || identityRequest.contact().phoneNumber().isBlank()) {
            throw new ApplicationException(400, "O telefone é obrigatório");
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public void validateIdentityUniqueness(Identity identityRequest) {
        if (contactRepository.findByContactEmail(identityRequest.contact().email()).isPresent()) {
            throw new ApplicationException(409, "Já existe um usuário cadastrado com este e-mail");
        }

        if (identityRepository.existsByDocumento(new Documento(identityRequest.documento()).valor())) {
            throw new ApplicationException(409, "Já existe um usuário cadastrado com este documento");
        }

    }
}
