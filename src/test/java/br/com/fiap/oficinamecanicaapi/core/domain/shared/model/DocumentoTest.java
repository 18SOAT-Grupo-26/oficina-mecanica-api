package br.com.fiap.oficinamecanicaapi.core.domain.shared.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentoTest {

    private static final String CPF = "52998224725";
    private static final String CPF_COM_MASCARA = "529.982.247-25";
    private static final String CNPJ_NUMERICO_COM_MASCARA = "11.222.333/0001-81";
    private static final String CNPJ_ALFANUMERICO = "12ABC34501DE35";
    private static final String CNPJ_ALFANUMERICO_MINUSCULO = "12.abc.345/01de-35";
    private static final String MENSAGEM_INVALIDO = "Documento inválido — informe um CPF ou CNPJ válido";

    @Test
    @DisplayName("Deve criar documento CPF normalizado a partir do valor com máscara")
    void deveCriarCpfComMascara() {
        Documento documento = new Documento(CPF_COM_MASCARA);

        assertThat(documento.valor()).isEqualTo(CPF);
        assertThat(documento.tipo()).isEqualTo(TipoDocumento.CPF);
    }

    @Test
    @DisplayName("Deve criar documento CNPJ numérico normalizado")
    void deveCriarCnpjNumerico() {
        Documento documento = new Documento(CNPJ_NUMERICO_COM_MASCARA);

        assertThat(documento.valor()).isEqualTo("11222333000181");
        assertThat(documento.tipo()).isEqualTo(TipoDocumento.CNPJ);
    }

    @Test
    @DisplayName("Deve criar documento CNPJ alfanumérico convertendo para maiúsculas")
    void deveCriarCnpjAlfanumerico() {
        Documento documento = new Documento(CNPJ_ALFANUMERICO_MINUSCULO);

        assertThat(documento.valor()).isEqualTo(CNPJ_ALFANUMERICO);
        assertThat(documento.tipo()).isEqualTo(TipoDocumento.CNPJ);
    }

    @Test
    @DisplayName("Documentos com o mesmo valor normalizado devem ser iguais")
    void deveSerIgualPorValor() {
        assertThat(new Documento(CPF_COM_MASCARA)).isEqualTo(new Documento(CPF));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "52998224724",      // CPF com dígito verificador errado
            "11111111111",      // CPF com dígitos repetidos
            "5299822472A",      // CPF com letra
            "11222333000182",   // CNPJ com dígito verificador errado
            "123456789",        // tamanho que não é CPF nem CNPJ
            "   "
    })
    @DisplayName("Deve rejeitar documento inválido com erro 400")
    void deveRejeitarDocumentoInvalido(String valor) {
        DadoInvalidoException exception = assertThrows(DadoInvalidoException.class, () -> new Documento(valor));

        assertThat(exception.getMessage()).isEqualTo(MENSAGEM_INVALIDO);
    }
}
