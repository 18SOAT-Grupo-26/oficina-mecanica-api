package br.com.fiap.oficinamecanicaapi.core.usecase.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.IdentidadeEntity;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioUseCaseTest {

    private static final String NOME_USUARIO = "admin";
    private static final String SENHA = "Admin@123";
    private static final Usuario USUARIO = new Usuario(NOME_USUARIO, SENHA, false, Perfil.ADMINISTRATOR);

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
        when(usuarioRepository.existsByNomeUsuario(NOME_USUARIO)).thenReturn(false);
        when(perfilRepository.findByNome(Perfil.ADMINISTRATOR.name())).thenReturn(Optional.of(perfilEntity));
        when(passwordEncoder.encode(SENHA)).thenReturn("cifrada");
        when(usuarioMapper.toEntityCadastro(any(Usuario.class), any(), any())).thenReturn(usuarioEntity);

        useCase.cadastrar(USUARIO, new IdentidadeEntity());

        verify(usuarioRepository).save(usuarioEntity);
    }

    @Test
    @DisplayName("Deve rejeitar com 409 nome de usuário já cadastrado")
    void deveRejeitarNomeUsuarioRepetido() {
        when(usuarioRepository.existsByNomeUsuario(NOME_USUARIO)).thenReturn(true);

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> useCase.cadastrar(USUARIO, new IdentidadeEntity()));

        assertThat(ex.getErrorCode()).isEqualTo(409);
        verify(usuarioRepository, never()).save(any());
    }
}
