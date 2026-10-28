package br.com.fiap.oficinamecanicaapi.core.usecase.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.PerfilEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.UsuarioEntity;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    private static final Long CLIENTE_ID = 10L;

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
    @DisplayName("Deve cadastrar usuário CLIENTE ligado a um cliente ativo")
    void deveCadastrarCliente() {
        var cliente = usuario(Perfil.CLIENTE, CLIENTE_ID);
        var perfilEntity = new PerfilEntity();
        var usuarioEntity = new UsuarioEntity();
        prepararEmailENomeDisponiveis();
        when(clientePortOut.buscarAtivoPorId(CLIENTE_ID)).thenReturn(Optional.of(mock(Cliente.class)));
        when(usuarioRepository.existsByClienteId(CLIENTE_ID)).thenReturn(false);
        when(perfilRepository.findByNome(Perfil.CLIENTE.name())).thenReturn(Optional.of(perfilEntity));
        when(passwordEncoder.encode(SENHA)).thenReturn(SENHA_CIFRADA);
        when(usuarioMapper.toEntityCadastro(
                new Usuario(NOME, EMAIL, NOME_USUARIO, SENHA_CIFRADA, false, Perfil.CLIENTE, CLIENTE_ID), perfilEntity))
                .thenReturn(usuarioEntity);

        useCase.cadastrar(cliente);

        verify(usuarioRepository).save(usuarioEntity);
    }

    @Test
    @DisplayName("Deve rejeitar com 400 usuário CLIENTE sem clienteId")
    void deveRejeitarClienteSemClienteId() {
        prepararEmailENomeDisponiveis();

        assertCadastroRecusado(usuario(Perfil.CLIENTE, null), 400);
    }

    @Test
    @DisplayName("Deve rejeitar com 400 clienteId em perfil diferente de CLIENTE")
    void deveRejeitarClienteIdEmOutroPerfil() {
        prepararEmailENomeDisponiveis();

        assertCadastroRecusado(usuario(Perfil.FUNCIONARIO, CLIENTE_ID), 400);
        verifyNoInteractions(clientePortOut);
    }

    @Test
    @DisplayName("Deve rejeitar com 404 usuário CLIENTE de cliente inexistente ou inativo")
    void deveRejeitarClienteInexistente() {
        prepararEmailENomeDisponiveis();
        when(clientePortOut.buscarAtivoPorId(CLIENTE_ID)).thenReturn(Optional.empty());

        assertCadastroRecusado(usuario(Perfil.CLIENTE, CLIENTE_ID), 404);
    }

    @Test
    @DisplayName("Deve rejeitar com 409 segundo usuário para o mesmo cliente")
    void deveRejeitarClienteComUsuario() {
        prepararEmailENomeDisponiveis();
        when(clientePortOut.buscarAtivoPorId(CLIENTE_ID)).thenReturn(Optional.of(mock(Cliente.class)));
        when(usuarioRepository.existsByClienteId(CLIENTE_ID)).thenReturn(true);

        assertCadastroRecusado(usuario(Perfil.CLIENTE, CLIENTE_ID), 409);
    }

    @Test
    @DisplayName("Deve informar se já existe usuário com perfil ADMIN")
    void deveInformarSeExisteAdministrador() {
        when(usuarioRepository.existsByPerfil_Nome(Perfil.ADMIN.name())).thenReturn(true);

        assertThat(useCase.existeAdministrador()).isTrue();
    }

    private static Usuario usuario(Perfil perfil, Long clienteId) {
        return new Usuario(NOME, EMAIL, NOME_USUARIO, SENHA, false, perfil, clienteId);
    }

    private void prepararEmailENomeDisponiveis() {
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(usuarioRepository.existsByNomeUsuario(NOME_USUARIO)).thenReturn(false);
    }

    private void assertCadastroRecusado(Usuario usuario, int status) {
        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.cadastrar(usuario));

        assertThat(ex.getErrorCode()).isEqualTo(status);
        verify(usuarioRepository, never()).save(any());
    }
}
