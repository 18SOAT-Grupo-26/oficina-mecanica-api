package br.com.fiap.oficinamecanicaapi.core.domain.cliente.model;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Endereco;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteTest {

    private static final Documento CPF = new Documento("529.982.247-25");
    private static final Endereco ENDERECO = new Endereco("Av. Paulista", "1000", "Bela Vista", "São Paulo",
            "SP", "01310100", null);
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-21T10:00:00-03:00");

    @Test
    @DisplayName("Deve criar cliente novo ativo, sem id, com nome, e-mail e telefone sem espaços nas pontas")
    void deveCriarClienteNovo() {
        Cliente cliente = Cliente.novo("  Maria da Silva ", CPF, " maria@email.com ", " 11999998888 ", ENDERECO);

        assertThat(cliente.getId()).isNull();
        assertThat(cliente.isAtivo()).isTrue();
        assertThat(cliente.getNome()).isEqualTo("Maria da Silva");
        assertThat(cliente.getEmail()).isEqualTo("maria@email.com");
        assertThat(cliente.getTelefone()).isEqualTo("11999998888");
        assertThat(cliente.getDocumento()).isEqualTo(CPF);
        assertThat(cliente.getEndereco()).isEqualTo(ENDERECO);
        assertThat(cliente.getCriadoEm()).isNull();
    }

    @Test
    @DisplayName("Deve reconstituir cliente persistido mantendo id, situação e datas")
    void deveReconstituir() {
        Cliente cliente = Cliente.reconstituir(7L, "Maria", CPF, "maria@email.com", "11999998888", null,
                false, CRIADO_EM, CRIADO_EM);

        assertThat(cliente.getId()).isEqualTo(7L);
        assertThat(cliente.isAtivo()).isFalse();
        assertThat(cliente.getCriadoEm()).isEqualTo(CRIADO_EM);
        assertThat(cliente.getAtualizadoEm()).isEqualTo(CRIADO_EM);
    }

    @Test
    @DisplayName("Deve atualizar os dados e desativar")
    void deveAtualizarEDesativar() {
        Cliente cliente = Cliente.novo("Maria", CPF, "maria@email.com", "11999998888", null);
        Documento cnpj = new Documento("11.222.333/0001-81");

        cliente.atualizar("Oficina da Maria", cnpj, "contato@oficina.com", "1133334444", ENDERECO);
        cliente.desativar();

        assertThat(cliente.getNome()).isEqualTo("Oficina da Maria");
        assertThat(cliente.getDocumento()).isEqualTo(cnpj);
        assertThat(cliente.getEndereco()).isEqualTo(ENDERECO);
        assertThat(cliente.isAtivo()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("Deve rejeitar nome vazio com 400")
    void deveRejeitarNomeVazio(String nome) {
        assertErro400("O nome do cliente é obrigatório",
                () -> Cliente.novo(nome, CPF, "maria@email.com", "11999998888", null));
    }

    @Test
    @DisplayName("Deve rejeitar documento ausente com 400")
    void deveRejeitarDocumentoAusente() {
        assertErro400("O documento do cliente é obrigatório",
                () -> Cliente.novo("Maria", null, "maria@email.com", "11999998888", null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"maria", "maria@", "maria@email", "@email.com"})
    @DisplayName("Deve rejeitar e-mail inválido com 400")
    void deveRejeitarEmailInvalido(String email) {
        assertErro400("E-mail inválido — deve conter '@' e um domínio válido",
                () -> Cliente.novo("Maria", CPF, email, "11999998888", null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    @DisplayName("Deve rejeitar telefone vazio com 400")
    void deveRejeitarTelefoneVazio(String telefone) {
        assertErro400("O telefone do cliente é obrigatório",
                () -> Cliente.novo("Maria", CPF, "maria@email.com", telefone, null));
    }

    @Test
    @DisplayName("Deve manter os dados anteriores quando a atualização é inválida")
    void deveManterDadosQuandoAtualizacaoInvalida() {
        Cliente cliente = Cliente.novo("Maria", CPF, "maria@email.com", "11999998888", null);

        assertThatThrownBy(() -> cliente.atualizar("Outro nome", CPF, "invalido", "11999998888", null))
                .isInstanceOf(ApplicationException.class);

        assertThat(cliente.getNome()).isEqualTo("Maria");
    }

    private static void assertErro400(String mensagem, org.assertj.core.api.ThrowableAssert.ThrowingCallable acao) {
        assertThatThrownBy(acao)
                .isInstanceOf(ApplicationException.class)
                .hasMessage(mensagem)
                .extracting("errorCode").isEqualTo(400);
    }
}
