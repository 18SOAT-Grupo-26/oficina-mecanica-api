package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Identity;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity.ContactRepository;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity.IdentityRepository;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.CpfValidator;
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
            throw new ApplicationException(400, "Contact information is required");
        }
        if (!isValidEmail(identityRequest.contact().email())) {
            throw new ApplicationException(400, "Email is invalid — it must contain '@' and a valid domain");
        }
        if (identityRequest.fullName() == null || identityRequest.fullName().isBlank()) {
            throw new ApplicationException(400, "Full name is required");
        }
        if (identityRequest.givenName() == null || identityRequest.givenName().isBlank()) {
            throw new ApplicationException(400, "Given name is required");
        }
        // CPF: accept the user's typed value (possibly with mask), normalize
        // to digits, and verify with the official check-digit algorithm.
        String docDigits = CpfValidator.digits(identityRequest.cpf());
        if (docDigits == null || docDigits.isBlank()) {
            throw new ApplicationException(400, "CPF is required");
        }
        if (!CpfValidator.isValid(docDigits)) {
            throw new ApplicationException(400, "Invalid CPF — please double-check the digits");
        }

        if (identityRequest.contact().phoneNumber() == null
                || identityRequest.contact().phoneNumber().isBlank()) {
            throw new ApplicationException(400, "Phone number is required");
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public void validateIdentityUniqueness(Identity identityRequest) {
        if (contactRepository.findByContactEmail(identityRequest.contact().email()).isPresent()) {
            throw new ApplicationException(409, "Email already in use");
        }
        if (identityRequest.cpf() != null && !identityRequest.cpf().isBlank()
                && identityRepository.existsByCpf(identityRequest.cpf().trim())) {
            throw new ApplicationException(409, "Document already in use by another user");
        }

    }
}
