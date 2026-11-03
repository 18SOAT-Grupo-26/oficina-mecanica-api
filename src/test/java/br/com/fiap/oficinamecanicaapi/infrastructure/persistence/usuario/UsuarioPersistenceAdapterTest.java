package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioPersistenceAdapterTest {

    private static final Usuario CLIENTE =
            new Usuario("Ana", "ana@oficina.com.br", "ana", "{bcrypt}cifrada", false, Perfil.CLIENTE, 7L);

    @Mock
    private UsuarioJpaRepository usuarioRepository;

    @Mock
    private PerfilJpaRepository perfilRepository;

    private UsuarioPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UsuarioPersistenceAdapter(usuarioRepository, perfilRepository,
                Mappers.getMapper(UsuarioPersistenceMapper.class));
    }

    @Test
    @DisplayName("cadastrar deve gravar o usuário ativo com o perfil da tabela de perfis")
    void cadastrar() {
        PerfilEntity perfil = perfil();
        when(perfilRepository.findByNome("CLIENTE")).thenReturn(Optional.of(perfil));

        adapter.cadastrar(CLIENTE);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        UsuarioEntity gravado = captor.getValue();
        assertThat(gravado.getNomeUsuario()).isEqualTo("ana");
        assertThat(gravado.getSenha()).isEqualTo("{bcrypt}cifrada");
        assertThat(gravado.getPerfil()).isSameAs(perfil);
        assertThat(gravado.getClienteId()).isEqualTo(7L);
        assertThat(gravado.isAtivo()).isTrue();
        assertThat(gravado.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("cadastrar deve falhar como erro de servidor (500) quando o perfil não existe na tabela, sem gravar")
    void cadastrarComPerfilInexistente() {
        when(perfilRepository.findByNome("CLIENTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.cadastrar(CLIENTE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Perfil não encontrado: CLIENTE");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("buscarPorEmail e buscarPorNomeUsuario devem converter a entidade com perfil e permissões")
    void buscar() {
        UsuarioEntity entity = new UsuarioEntity();
        entity.setNomeUsuario("ana");
        entity.setPerfil(perfil());
        when(usuarioRepository.findByEmail("ana@oficina.com.br")).thenReturn(Optional.of(entity));
        when(usuarioRepository.findByNomeUsuario("ana")).thenReturn(Optional.empty());

        DetalhesUsuario detalhes = adapter.buscarPorEmail("ana@oficina.com.br").orElseThrow();

        assertThat(detalhes.usuario().nomeUsuario()).isEqualTo("ana");
        assertThat(detalhes.usuario().perfil()).isEqualTo(Perfil.CLIENTE);
        assertThat(detalhes.permissoes()).containsExactly("OS_LER");
        assertThat(adapter.buscarPorNomeUsuario("ana")).isEmpty();
    }

    @Test
    @DisplayName("as consultas de existência devem repassar o resultado do repositório")
    void existencias() {
        when(usuarioRepository.existsByEmail("ana@oficina.com.br")).thenReturn(true);
        when(usuarioRepository.existsByNomeUsuario("ana")).thenReturn(false);
        when(usuarioRepository.existsByClienteId(7L)).thenReturn(true);
        when(usuarioRepository.existsByPerfil_Nome("ADMIN")).thenReturn(true);

        assertThat(adapter.existePorEmail("ana@oficina.com.br")).isTrue();
        assertThat(adapter.existePorNomeUsuario("ana")).isFalse();
        assertThat(adapter.existePorClienteId(7L)).isTrue();
        assertThat(adapter.existePorPerfil(Perfil.ADMIN)).isTrue();
    }

    private static PerfilEntity perfil() {
        PermissaoEntity permissao = new PermissaoEntity();
        permissao.setNome("OS_LER");
        PerfilEntity perfil = new PerfilEntity();
        perfil.setNome("CLIENTE");
        perfil.setPermissoes(Set.of(permissao));
        return perfil;
    }
}
