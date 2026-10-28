package br.com.fiap.oficinamecanicaapi.infrastructure.config;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministradorInicialTest {

    private static final String NOME = "Administrador";
    private static final String EMAIL = "admin@oficina.com.br";
    private static final String USUARIO = "admin";
    private static final String SENHA = "Senha@Forte123";
    private static final Usuario ADMINISTRADOR = new Usuario(NOME, EMAIL, USUARIO, SENHA, false, Perfil.ADMINISTRATOR);

    @Mock
    private UsuarioUseCasePortIn usuarioUseCase;

    @Test
    @DisplayName("Deve criar o administrador com os valores das variáveis quando ainda não existir nenhum")
    void deveCriarAdministrador() {
        when(usuarioUseCase.existeAdministrador()).thenReturn(false);

        administrador(EMAIL, USUARIO, SENHA).run(null);

        verify(usuarioUseCase).cadastrar(ADMINISTRADOR);
    }

    @Test
    @DisplayName("Não deve criar outro administrador quando já existir um")
    void naoDeveCriarQuandoJaExiste() {
        when(usuarioUseCase.existeAdministrador()).thenReturn(true);

        administrador(EMAIL, USUARIO, SENHA).run(null);

        verify(usuarioUseCase, never()).cadastrar(any());
    }

    @ParameterizedTest
    @CsvSource(value = {"'', admin, Senha@Forte123", "admin@oficina.com.br, '', Senha@Forte123",
            "admin@oficina.com.br, admin, ''", "NULO, NULO, NULO"}, nullValues = "NULO")
    @DisplayName("Não deve criar o administrador quando alguma variável ADMIN_* estiver ausente")
    void naoDeveCriarSemVariaveis(String email, String usuario, String senha) {
        when(usuarioUseCase.existeAdministrador()).thenReturn(false);

        administrador(email, usuario, senha).run(null);

        verify(usuarioUseCase, never()).cadastrar(any());
    }

    @Test
    @DisplayName("Deve falhar a subida quando o cadastro do administrador for recusado (ex.: senha fraca)")
    void deveFalharQuandoRecusado() {
        when(usuarioUseCase.existeAdministrador()).thenReturn(false);
        doThrow(new ApplicationException(400, "A senha deve conter uma letra maiúscula"))
                .when(usuarioUseCase).cadastrar(ADMINISTRADOR);
        AdministradorInicial administradorInicial = administrador(EMAIL, USUARIO, SENHA);

        assertThatThrownBy(() -> administradorInicial.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("A senha deve conter uma letra maiúscula");
    }

    @Test
    @DisplayName("Deve seguir normal quando outra instância criar o administrador ao mesmo tempo")
    void deveIgnorarCriacaoConcorrente() {
        when(usuarioUseCase.existeAdministrador()).thenReturn(false, true);
        doThrow(new ApplicationException(409, "Já existe um usuário cadastrado com este e-mail"))
                .when(usuarioUseCase).cadastrar(ADMINISTRADOR);

        AdministradorInicial administradorInicial = administrador(EMAIL, USUARIO, SENHA);

        assertDoesNotThrow(() -> administradorInicial.run(null));

        verify(usuarioUseCase).cadastrar(ADMINISTRADOR);
    }

    private AdministradorInicial administrador(String email, String usuario, String senha) {
        return new AdministradorInicial(usuarioUseCase, NOME, email, usuario, senha);
    }
}
