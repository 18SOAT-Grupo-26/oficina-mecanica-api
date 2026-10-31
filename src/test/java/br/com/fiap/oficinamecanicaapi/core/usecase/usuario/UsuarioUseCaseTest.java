package br.com.fiap.oficinamecanicaapi.core.usecase.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.usuario.UsuarioPortOut;
import br.com.fiap.oficinamecanicaapi.shared.exception.ConflitoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficinamecanicaapi.shared.utils.helpers.PoliticaSenha;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioUseCaseTest {

    private static final String NOME = "Ana Administradora";
    private static final String EMAIL = "ana@oficina.com.br";
    private static final String NOME_USUARIO = "admin";
    private static final String SENHA = "Admin@123";
    private static final String SENHA_CIFRADA = "cifrada";
    private static final Usuario USUARIO = new Usuario(NOME, EMAIL, NOME_USUARIO, SENHA, false, Perfil.ADMIN);
    private static final Usuario USUARIO_CIFRADO =
            new Usuario(NOME, EMAIL, NOME_USUARIO, SENHA_CIFRADA, false, Perfil.ADMIN);
    private static final DetalhesUsuario DETALHES = new DetalhesUsuario(USUARIO, Set.of());

    @Mock
    private UsuarioPortOut usuarioPortOut;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PoliticaSenha politicaSenha;

    @Mock
    private ClientePortOut clientePortOut;

    private UsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UsuarioUseCase(usuarioPortOut, new UsuarioPayload(),
                passwordEncoder, politicaSenha, clientePortOut);
    }

    @Test
    @DisplayName("Deve cadastrar usuário com nome de usuário disponível")
    void deveCadastrarUsuario() {
        when(usuarioPortOut.existePorEmail(EMAIL)).thenReturn(false);
        when(usuarioPortOut.existePorNomeUsuario(NOME_USUARIO)).thenReturn(false);
        when(passwordEncoder.encode(SENHA)).thenReturn(SENHA_CIFRADA);

        useCase.cadastrar(USUARIO);

        verify(usuarioPortOut).cadastrar(USUARIO_CIFRADO);
    }

    @Test
    @DisplayName("Deve rejeitar com 409 e-mail já cadastrado")
    void deveRejeitarEmailRepetido() {
        when(usuarioPortOut.existePorEmail(EMAIL)).thenReturn(true);

        ConflitoException ex = assertThrows(ConflitoException.class, () -> useCase.cadastrar(USUARIO));

        verify(usuarioPortOut, never()).cadastrar(any());
    }

    @Test
    @DisplayName("Deve rejeitar com 409 nome de usuário já cadastrado")
    void deveRejeitarNomeUsuarioRepetido() {
        when(usuarioPortOut.existePorEmail(EMAIL)).thenReturn(false);
        when(usuarioPortOut.existePorNomeUsuario(NOME_USUARIO)).thenReturn(true);

        ConflitoException ex = assertThrows(ConflitoException.class, () -> useCase.cadastrar(USUARIO));

        verify(usuarioPortOut, never()).cadastrar(any());
    }

    @Test
    @DisplayName("Deve informar se já existe usuário com perfil ADMIN")
    void deveInformarSeExisteAdministrador() {
        when(usuarioPortOut.existePorPerfil(Perfil.ADMIN)).thenReturn(true);

        assertThat(useCase.existeAdministrador()).isTrue();
    }

    @Test
    @DisplayName("Deve buscar pelo nome de usuário quando o identificador não tem @")
    void deveBuscarPorNomeUsuario() {
        when(usuarioPortOut.buscarPorNomeUsuario("ana")).thenReturn(Optional.of(DETALHES));

        assertThat(useCase.buscarPorIdentificador("ana")).isEqualTo(DETALHES);
        verify(usuarioPortOut, never()).buscarPorEmail(any());
    }

    @Test
    @DisplayName("Deve buscar pelo e-mail quando o identificador tem @")
    void deveBuscarPorEmail() {
        when(usuarioPortOut.buscarPorEmail("ana@oficina.com.br")).thenReturn(Optional.of(DETALHES));

        assertThat(useCase.buscarPorIdentificador("ana@oficina.com.br")).isEqualTo(DETALHES);
        verify(usuarioPortOut, never()).buscarPorNomeUsuario(any());
    }

    @Test
    @DisplayName("Deve responder 404 quando o usuário não existe, por nome ou por e-mail")
    void deveResponderNaoEncontrado() {
        when(usuarioPortOut.buscarPorNomeUsuario("fantasma")).thenReturn(Optional.empty());
        when(usuarioPortOut.buscarPorEmail("fantasma@oficina.com")).thenReturn(Optional.empty());

        assertNaoEncontrado("fantasma");
        assertNaoEncontrado("fantasma@oficina.com");
    }

    @Test
    @DisplayName("Falha de infraestrutura na busca deve subir até o handler, e não virar 404 (usuário não encontrado)")
    void devePropagarFalhaNaBusca() {
        when(usuarioPortOut.buscarPorNomeUsuario("ana")).thenThrow(new IllegalStateException("banco caiu"));

        assertThatThrownBy(() -> useCase.buscarPorIdentificador("ana"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("banco caiu");
    }

    @Test
    @DisplayName("Deve rejeitar com 400 o cadastro com dados inválidos antes de consultar o banco")
    void deveRejeitarPayloadInvalido() {
        Usuario semNome = new Usuario(" ", "ana@oficina.com.br", "ana", "Admin@123", false, Perfil.FUNCIONARIO);

        assertThatThrownBy(() -> useCase.cadastrar(semNome))
                .isInstanceOf(DadoInvalidoException.class);
        verifyNoInteractions(usuarioPortOut, politicaSenha);
    }

    @Test
    @DisplayName("Deve rejeitar o cadastro quando a senha não segue a política, sem gravar")
    void deveRejeitarSenhaFraca() {
        doThrow(new DadoInvalidoException("A senha deve conter um número")).when(politicaSenha).validar("Admin@123");

        assertThatThrownBy(() -> useCase.cadastrar(USUARIO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("A senha deve conter um número");
        verify(usuarioPortOut, never()).cadastrar(any());
    }

    @Test
    @DisplayName("Deve propagar falha inesperada ao gravar")
    void devePropagarFalhaAoGravar() {
        when(usuarioPortOut.existePorEmail(USUARIO.email())).thenReturn(false);
        when(usuarioPortOut.existePorNomeUsuario(USUARIO.nomeUsuario())).thenReturn(false);
        when(passwordEncoder.encode("Admin@123")).thenReturn("{bcrypt}cifrada");
        doThrow(new IllegalStateException("violação de constraint")).when(usuarioPortOut).cadastrar(any());

        assertThatThrownBy(() -> useCase.cadastrar(USUARIO)).isInstanceOf(IllegalStateException.class);
    }

    private void assertNaoEncontrado(String identificador) {
        assertThatThrownBy(() -> useCase.buscarPorIdentificador(identificador))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Usuário não encontrado");
    }
}
