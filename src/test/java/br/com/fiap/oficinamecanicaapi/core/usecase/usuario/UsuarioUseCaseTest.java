package br.com.fiap.oficinamecanicaapi.core.usecase.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.PerfilEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.entity.UsuarioEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
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
    private static final Usuario USUARIO = new Usuario(NOME, EMAIL, NOME_USUARIO, SENHA, false, Perfil.ADMINISTRATOR);
    private static final Usuario USUARIO_CIFRADO =
            new Usuario(NOME, EMAIL, NOME_USUARIO, SENHA_CIFRADA, false, Perfil.ADMINISTRATOR);

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

    private UsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UsuarioUseCase(usuarioRepository, perfilRepository, usuarioMapper, new UsuarioPayload(),
                passwordEncoder, politicaSenha);
    }

    @Test
    @DisplayName("Deve cadastrar usuário com nome de usuário disponível")
    void deveCadastrarUsuario() {
        var perfilEntity = new PerfilEntity();
        var usuarioEntity = new UsuarioEntity();
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(usuarioRepository.existsByNomeUsuario(NOME_USUARIO)).thenReturn(false);
        when(perfilRepository.findByNome(Perfil.ADMINISTRATOR.name())).thenReturn(Optional.of(perfilEntity));
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
    @DisplayName("Deve informar se já existe usuário com perfil ADMINISTRATOR")
    void deveInformarSeExisteAdministrador() {
        when(usuarioRepository.existsByPerfil_Nome(Perfil.ADMINISTRATOR.name())).thenReturn(true);

        assertThat(useCase.existeAdministrador()).isTrue();
    }
}
