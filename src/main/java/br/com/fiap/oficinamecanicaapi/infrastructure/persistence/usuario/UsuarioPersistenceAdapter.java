package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.out.usuario.UsuarioPortOut;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class UsuarioPersistenceAdapter implements UsuarioPortOut {

    private final UsuarioJpaRepository usuarioRepository;
    private final PerfilJpaRepository perfilRepository;
    private final UsuarioPersistenceMapper mapper;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository usuarioRepository,
                                     PerfilJpaRepository perfilRepository,
                                     UsuarioPersistenceMapper mapper) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void cadastrar(Usuario usuario) {
        var perfilEntity = perfilRepository.findByNome(usuario.perfil().name())
                .orElseThrow(() -> new ApplicationException(400,
                        "Perfil não encontrado: " + usuario.perfil().name()));
        usuarioRepository.save(mapper.toEntityCadastro(usuario, perfilEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DetalhesUsuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email).map(mapper::toDetalhesUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DetalhesUsuario> buscarPorNomeUsuario(String nomeUsuario) {
        return usuarioRepository.findByNomeUsuario(nomeUsuario).map(mapper::toDetalhesUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorEmail(String email) {
        return usuarioRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorNomeUsuario(String nomeUsuario) {
        return usuarioRepository.existsByNomeUsuario(nomeUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorClienteId(Long clienteId) {
        return usuarioRepository.existsByClienteId(clienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorPerfil(Perfil perfil) {
        return usuarioRepository.existsByPerfil_Nome(perfil.name());
    }
}
