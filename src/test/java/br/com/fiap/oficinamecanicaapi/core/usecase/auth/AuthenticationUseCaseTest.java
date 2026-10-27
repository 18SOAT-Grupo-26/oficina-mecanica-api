package br.com.fiap.oficinamecanicaapi.core.usecase.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Login sem enumeração de usuários: usuário inexistente e senha errada respondem igual (401).
 * Usa @InjectMocks e mocks dos records para não depender dos construtores, que mudam com o modelo de usuário.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationUseCaseTest {

    private static final String IDENTIFICADOR = "admin";
    private static final String SENHA = "Admin@123";
    private static final String SENHA_CIFRADA = "{bcrypt}cifrada";
    private static final String TOKEN = "jwt";
    private static final String MENSAGEM = "Credenciais inválidas";
    private static final SigninRequest REQUEST = new SigninRequest(IDENTIFICADOR, SENHA);

    @Mock
    private UsuarioUseCasePortIn usuarioService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProviderUseCase jwtTokenProvider;

    @InjectMocks
    private AuthenticationUseCase useCase;

    @Test
    @DisplayName("Deve gerar o token quando usuário e senha conferem")
    void deveAutenticar() {
        var detalhes = detalhesComSenha(SENHA_CIFRADA);
        when(usuarioService.buscarPorIdentificador(IDENTIFICADOR)).thenReturn(detalhes);
        when(passwordEncoder.matches(SENHA, SENHA_CIFRADA)).thenReturn(true);
        when(jwtTokenProvider.gerarToken(detalhes)).thenReturn(TOKEN);

        AuthToken authToken = useCase.autenticar(REQUEST);

        assertThat(authToken.token()).isEqualTo("Bearer " + TOKEN);
    }

    @Test
    @DisplayName("Deve responder 401 com a mensagem genérica quando a senha estiver errada")
    void deveRecusarSenhaErrada() {
        var detalhes = detalhesComSenha(SENHA_CIFRADA);
        when(usuarioService.buscarPorIdentificador(IDENTIFICADOR)).thenReturn(detalhes);
        when(passwordEncoder.matches(SENHA, SENHA_CIFRADA)).thenReturn(false);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.autenticar(REQUEST));

        assertThat(ex.getErrorCode()).isEqualTo(401);
        assertThat(ex.getMessage()).isEqualTo(MENSAGEM);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("Deve responder igual à senha errada (401) quando o usuário não existir, sem revelar isso")
    void deveRecusarUsuarioInexistenteComoSenhaErrada() {
        when(usuarioService.buscarPorIdentificador(IDENTIFICADOR))
                .thenThrow(new ApplicationException(404, "Usuário não encontrado"));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.autenticar(REQUEST));

        assertThat(ex.getErrorCode()).isEqualTo(401);
        assertThat(ex.getMessage()).isEqualTo(MENSAGEM);
        // custo de bcrypt equivalente ao da senha errada: o tempo de resposta também não denuncia
        verify(passwordEncoder).encode(SENHA);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("Deve propagar erros que não sejam de usuário inexistente")
    void devePropagarOutrosErros() {
        when(usuarioService.buscarPorIdentificador(IDENTIFICADOR))
                .thenThrow(new ApplicationException(500, "Erro interno inesperado"));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.autenticar(REQUEST));

        assertThat(ex.getErrorCode()).isEqualTo(500);
        verify(passwordEncoder, never()).encode(SENHA);
    }

    private static DetalhesUsuario detalhesComSenha(String senhaCifrada) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.senha()).thenReturn(senhaCifrada);
        DetalhesUsuario detalhes = mock(DetalhesUsuario.class);
        when(detalhes.usuario()).thenReturn(usuario);
        return detalhes;
    }
}
