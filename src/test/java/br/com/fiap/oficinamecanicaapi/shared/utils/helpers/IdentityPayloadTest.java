package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.ContactEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Address;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Contact;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Identity;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity.ContactRepository;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identity.IdentityRepository;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityPayloadTest {

    private static final String CPF = "52998224725";
    private static final String CPF_COM_MASCARA = "529.982.247-25";
    private static final String CNPJ = "12ABC34501DE35";
    private static final String CNPJ_COM_MASCARA = "12.abc.345/01de-35";
    private static final String CPF_INVALIDO = "52998224726";
    private static final String EMAIL = "admin@oficina.com";
    private static final Contact CONTACT = new Contact("11999998888", EMAIL);
    private static final Address ADDRESS = new Address("Rua A", "100", "Centro", "São Paulo", "SP", "01000000", null);

    @Mock
    private IdentityRepository identityRepository;

    @Mock
    private ContactRepository contactRepository;

    private IdentityPayload identityPayload;

    @BeforeEach
    void setUp() {
        identityPayload = new IdentityPayload(identityRepository, contactRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {CPF, CPF_COM_MASCARA, CNPJ, CNPJ_COM_MASCARA})
    @DisplayName("Deve aceitar documento CPF ou CNPJ válido, com ou sem máscara")
    void deveAceitarDocumentoValido(String documento) {
        assertDoesNotThrow(() -> identityPayload.validateIdentityPayload(identity(documento)));
    }

    @ParameterizedTest
    @ValueSource(strings = {CPF_INVALIDO, "1234", " "})
    @DisplayName("Deve rejeitar com 400 documento inválido ou em branco")
    void deveRejeitarDocumentoInvalido(String documento) {
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> identityPayload.validateIdentityPayload(identity(documento)));

        assertThat(exception.getErrorCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve rejeitar com 409 documento já cadastrado, consultando pelo valor normalizado")
    void deveRejeitarDocumentoDuplicado() {
        when(contactRepository.findByContactEmail(EMAIL)).thenReturn(Optional.empty());
        when(identityRepository.existsByDocumento(CNPJ)).thenReturn(true);

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> identityPayload.validateIdentityUniqueness(identity(CNPJ_COM_MASCARA)));

        assertThat(exception.getErrorCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("Deve aceitar documento ainda não cadastrado")
    void deveAceitarDocumentoNaoCadastrado() {
        when(contactRepository.findByContactEmail(EMAIL)).thenReturn(Optional.empty());
        when(identityRepository.existsByDocumento(CPF)).thenReturn(false);

        assertDoesNotThrow(() -> identityPayload.validateIdentityUniqueness(identity(CPF_COM_MASCARA)));
    }

    private static Identity identity(String documento) {
        return new Identity("Administrador da Oficina", "Admin", documento, "123456789",
                LocalDate.of(1990, 1, 1), CONTACT, ADDRESS);
    }
}
