package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsuarioPayloadTest {

    private static final String NOME = "Ana Administradora";
    private static final String EMAIL = "ana@oficina.com.br";
    private static final String NOME_USUARIO = "admin";
    private static final String SENHA = "Admin@123";

    private final UsuarioPayload usuarioPayload = new UsuarioPayload();

    @Test
    @DisplayName("Deve aceitar usuário com nome, e-mail e nome de usuário válidos")
    void deveAceitarUsuarioValido() {
        assertDoesNotThrow(() -> usuarioPayload.validarUsuario(usuario(NOME, EMAIL, NOME_USUARIO)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("Deve rejeitar com 400 usuário sem nome")
    void deveRejeitarUsuarioSemNome(String nome) {
        assertErro400(() -> usuarioPayload.validarUsuario(usuario(nome, EMAIL, NOME_USUARIO)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ana", "ana@oficina", "@oficina.com.br"})
    @DisplayName("Deve rejeitar com 400 e-mail ausente ou inválido")
    void deveRejeitarEmailInvalido(String email) {
        assertErro400(() -> usuarioPayload.validarUsuario(usuario(NOME, email, NOME_USUARIO)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("Deve rejeitar com 400 usuário sem nome de usuário")
    void deveRejeitarUsuarioSemNomeUsuario(String nomeUsuario) {
        assertErro400(() -> usuarioPayload.validarUsuario(usuario(NOME, EMAIL, nomeUsuario)));
    }

    private static Usuario usuario(String nome, String email, String nomeUsuario) {
        return new Usuario(nome, email, nomeUsuario, SENHA, false, Perfil.ADMIN);
    }

    private static void assertErro400(Executable chamada) {
        ApplicationException exception = assertThrows(ApplicationException.class, chamada);
        assertThat(exception.getErrorCode()).isEqualTo(400);
    }
}
