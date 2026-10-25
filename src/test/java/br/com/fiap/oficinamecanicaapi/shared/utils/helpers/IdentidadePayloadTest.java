package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.ContatoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Endereco;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Contato;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Identidade;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identidade.ContatoRepository;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identidade.IdentidadeRepository;
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
class IdentidadePayloadTest {

    private static final String CPF = "52998224725";
    private static final String CPF_COM_MASCARA = "529.982.247-25";
    private static final String CNPJ = "12ABC34501DE35";
    private static final String CNPJ_COM_MASCARA = "12.abc.345/01de-35";
    private static final String CPF_INVALIDO = "52998224726";
    private static final String EMAIL = "admin@oficina.com";
    private static final Contato CONTATO = new Contato("11999998888", EMAIL);
    private static final Endereco ENDERECO = new Endereco("Rua A", "100", "Centro", "São Paulo", "SP", "01000000", null);

    @Mock
    private IdentidadeRepository identidadeRepository;

    @Mock
    private ContatoRepository contatoRepository;

    private IdentidadePayload identidadePayload;

    @BeforeEach
    void setUp() {
        identidadePayload = new IdentidadePayload(identidadeRepository, contatoRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {CPF, CPF_COM_MASCARA, CNPJ, CNPJ_COM_MASCARA})
    @DisplayName("Deve aceitar documento CPF ou CNPJ válido, com ou sem máscara")
    void deveAceitarDocumentoValido(String documento) {
        assertDoesNotThrow(() -> identidadePayload.validarIdentidade(identidade(documento)));
    }

    @ParameterizedTest
    @ValueSource(strings = {CPF_INVALIDO, "1234", " "})
    @DisplayName("Deve rejeitar com 400 documento inválido ou em branco")
    void deveRejeitarDocumentoInvalido(String documento) {
        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> identidadePayload.validarIdentidade(identidade(documento)));

        assertThat(exception.getErrorCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve rejeitar com 409 documento já cadastrado, consultando pelo valor normalizado")
    void deveRejeitarDocumentoDuplicado() {
        when(contatoRepository.buscarIdentidadePorEmail(EMAIL)).thenReturn(Optional.empty());
        when(identidadeRepository.existsByDocumento(CNPJ)).thenReturn(true);

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> identidadePayload.validarUnicidade(identidade(CNPJ_COM_MASCARA)));

        assertThat(exception.getErrorCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("Deve aceitar documento ainda não cadastrado")
    void deveAceitarDocumentoNaoCadastrado() {
        when(contatoRepository.buscarIdentidadePorEmail(EMAIL)).thenReturn(Optional.empty());
        when(identidadeRepository.existsByDocumento(CPF)).thenReturn(false);

        assertDoesNotThrow(() -> identidadePayload.validarUnicidade(identidade(CPF_COM_MASCARA)));
    }

    private static Identidade identidade(String documento) {
        return new Identidade("Administrador da Oficina", "Admin", documento, "123456789",
                LocalDate.of(1990, 1, 1), CONTATO, ENDERECO);
    }
}
