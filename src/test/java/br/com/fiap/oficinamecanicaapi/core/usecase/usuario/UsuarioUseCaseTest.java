package br.com.fiap.oficinamecanicaapi.core.usecase.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.PerfilEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.UsuarioEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.usuario.mapper.UsuarioMapper;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.usuario.PerfilRepository;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.usuario.UsuarioRepository;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.helpers.PoliticaSenha;
import br.com.fiap.oficinamecanicaapi.shared.utils.helpers.UsuarioPayload;
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
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PoliticaSenha politicaSenha;

    @Mock
    private ClientePortOut clientePortOut;

    private UsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UsuarioUseCase(usuarioRepository, perfilRepository, usuarioMapper, new UsuarioPayload(),
                passwordEncoder, politicaSenha, clientePortOut);
    }

    @Test
    @DisplayName("Deve cadastrar usuário com nome de usuário disponível")
    void deveCadastrarUsuario() {
        var perfilEntity = new PerfilEntity();
        var usuarioEntity = new UsuarioEntity();
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(usuarioRepository.existsByNomeUsuario(NOME_USUARIO)).thenReturn(false);
        when(perfilRepository.findByNome(Perfil.ADMIN.name())).thenReturn(Optional.of(perfilEntity));
        when(passwordEncoder.encode(SENHA)).thenReturn(SENHA_CIFRADA);
        when(usuarioMapper.toEntityCadastro(USUARIO_CIFRADO, perfilEntity)).thenReturn(usuarioEntity);

        useCase.cadastrar(USUARIO);

        verify(usuarioRepository).save(usuarioEntity);
    }

    @Test
    @DisplayName("Deve rejeitar com 409 e-mail já cadastrado")
    void deveRejeitarEmailRepetido() {
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.cadastrar(USUARIO));

        assertThat(ex.getErrorCode()).isEqualTo(409);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar com 409 nome de usuário já cadastrado")
    void deveRejeitarNomeUsuarioRepetido() {
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(usuarioRepository.existsByNomeUsuario(NOME_USUARIO)).thenReturn(true);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.cadastrar(USUARIO));

        assertThat(ex.getErrorCode()).isEqualTo(409);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve informar se já existe usuário com perfil ADMIN")
    void deveInformarSeExisteAdministrador() {
        when(usuarioRepository.existsByPerfil_Nome(Perfil.ADMIN.name())).thenReturn(true);

        assertThat(useCase.existeAdministrador()).isTrue();
    }

    @Test
    @DisplayName("Deve buscar pelo nome de usuário quando o identificador não tem @")
    void deveBuscarPorNomeUsuario() {
        UsuarioEntity entity = new UsuarioEntity();
        when(usuarioRepository.findByNomeUsuario("ana")).thenReturn(Optional.of(entity));
        when(usuarioMapper.toDetalhesUsuario(entity)).thenReturn(DETALHES);

        assertThat(useCase.buscarPorIdentificador("ana")).isEqualTo(DETALHES);
        verify(usuarioRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Deve buscar pelo e-mail quando o identificador tem @")
    void deveBuscarPorEmail() {
        UsuarioEntity entity = new UsuarioEntity();
        when(usuarioRepository.findByEmail("ana@oficina.com.br")).thenReturn(Optional.of(entity));
        when(usuarioMapper.toDetalhesUsuario(entity)).thenReturn(DETALHES);

        assertThat(useCase.buscarPorIdentificador("ana@oficina.com.br")).isEqualTo(DETALHES);
        verify(usuarioRepository, never()).findByNomeUsuario(any());
    }

    @Test
    @DisplayName("Deve responder 404 quando o usuário não existe, por nome ou por e-mail")
    void deveResponderNaoEncontrado() {
        when(usuarioRepository.findByNomeUsuario("fantasma")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("fantasma@oficina.com")).thenReturn(Optional.empty());

        assertNaoEncontrado("fantasma");
        assertNaoEncontrado("fantasma@oficina.com");
    }

    @Test
    @DisplayName("Deve responder 404 também quando a busca falha por outro motivo, sem expor a causa")
    void deveEsconderFalhaNaBusca() {
        when(usuarioRepository.findByNomeUsuario("ana")).thenThrow(new IllegalStateException("banco caiu"));

        assertNaoEncontrado("ana");
    }

    @Test
    @DisplayName("Deve rejeitar com 400 o cadastro com perfil que não existe na tabela de perfis")
    void deveRejeitarPerfilInexistente() {
        when(usuarioRepository.existsByEmail(USUARIO.email())).thenReturn(false);
        when(usuarioRepository.existsByNomeUsuario(USUARIO.nomeUsuario())).thenReturn(false);
        when(perfilRepository.findByNome("ADMIN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.cadastrar(USUARIO))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Perfil não encontrado: ADMIN")
                .extracting("errorCode").isEqualTo(400);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar com 400 o cadastro com dados inválidos antes de consultar o banco")
    void deveRejeitarPayloadInvalido() {
        Usuario semNome = new Usuario(" ", "ana@oficina.com.br", "ana", "Admin@123", false, Perfil.FUNCIONARIO);

        assertThatThrownBy(() -> useCase.cadastrar(semNome))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(400);
        verifyNoInteractions(usuarioRepository, perfilRepository, politicaSenha);
    }

    @Test
    @DisplayName("Deve rejeitar o cadastro quando a senha não segue a política, sem gravar")
    void deveRejeitarSenhaFraca() {
        doThrow(new ApplicationException(400, "A senha deve conter um número")).when(politicaSenha).validar("Admin@123");

        assertThatThrownBy(() -> useCase.cadastrar(USUARIO))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("A senha deve conter um número");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve propagar falha inesperada ao gravar")
    void devePropagarFalhaAoGravar() {
        PerfilEntity perfil = new PerfilEntity();
        UsuarioEntity entity = new UsuarioEntity();
        when(usuarioRepository.existsByEmail(USUARIO.email())).thenReturn(false);
        when(usuarioRepository.existsByNomeUsuario(USUARIO.nomeUsuario())).thenReturn(false);
        when(perfilRepository.findByNome("ADMIN")).thenReturn(Optional.of(perfil));
        when(passwordEncoder.encode("Admin@123")).thenReturn("{bcrypt}cifrada");
        when(usuarioMapper.toEntityCadastro(any(Usuario.class), any(PerfilEntity.class))).thenReturn(entity);
        when(usuarioRepository.save(entity)).thenThrow(new IllegalStateException("violação de constraint"));

        assertThatThrownBy(() -> useCase.cadastrar(USUARIO)).isInstanceOf(IllegalStateException.class);
    }

    private void assertNaoEncontrado(String identificador) {
        assertThatThrownBy(() -> useCase.buscarPorIdentificador(identificador))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Usuário não encontrado")
                .extracting("errorCode").isEqualTo(404);
    }
}
