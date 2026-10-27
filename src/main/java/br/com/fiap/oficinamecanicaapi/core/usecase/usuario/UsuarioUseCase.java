package br.com.fiap.oficinamecanicaapi.core.usecase.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.usuario.mapper.UsuarioMapper;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.usuario.PerfilRepository;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.usuario.UsuarioRepository;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.helpers.PoliticaSenha;
import br.com.fiap.oficinamecanicaapi.shared.utils.helpers.UsuarioPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioUseCase implements UsuarioUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(UsuarioUseCase.class);
    private static final String MENSAGEM_NAO_ENCONTRADO = "Usuário não encontrado";

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final UsuarioMapper usuarioMapper;
    private final UsuarioPayload usuarioPayload;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaSenha politicaSenha;

    public UsuarioUseCase(UsuarioRepository usuarioRepository,
                          PerfilRepository perfilRepository,
                          UsuarioMapper usuarioMapper,
                          UsuarioPayload usuarioPayload,
                          PasswordEncoder passwordEncoder,
                          PoliticaSenha politicaSenha) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.usuarioMapper = usuarioMapper;
        this.usuarioPayload = usuarioPayload;
        this.passwordEncoder = passwordEncoder;
        this.politicaSenha = politicaSenha;
    }

    @Override
    @Transactional
    public void cadastrar(Usuario usuario) {
        try {
            validarPayloads(usuario);
            politicaSenha.validar(usuario.senha());
            validarEmailDisponivel(usuario.email());
            validarNomeUsuarioDisponivel(usuario.nomeUsuario());

            var perfilEntity = perfilRepository.findByNome(usuario.perfil().name())
                    .orElseThrow(() -> new ApplicationException(400,
                            "Perfil não encontrado: " + usuario.perfil().name()));

            var usuarioComSenhaCifrada = new Usuario(
                    usuario.nome(),
                    usuario.email(),
                    usuario.nomeUsuario(),
                    passwordEncoder.encode(usuario.senha()),
                    usuario.trocaSenhaObrigatoria(),
                    usuario.perfil()
            );

            usuarioRepository.save(usuarioMapper.toEntityCadastro(usuarioComSenhaCifrada, perfilEntity));
            log.info("action=user_register_success username={}", usuario.nomeUsuario());
        } catch (RuntimeException e) {
            log.error("action=user_register_failure username={} error={}",
                    usuario.nomeUsuario(), e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DetalhesUsuario buscarPorIdentificador(String identificador) {
        var tipoIdentificador = usuarioPayload.isEmail(identificador) ? "email" : "username";
        try {
            var entity = usuarioPayload.isEmail(identificador)
                    ? usuarioRepository.findByEmail(identificador)
                    .orElseThrow(() -> new ApplicationException(404, MENSAGEM_NAO_ENCONTRADO))
                    : usuarioRepository.findByNomeUsuario(identificador)
                    .orElseThrow(() -> new ApplicationException(404, MENSAGEM_NAO_ENCONTRADO));
            log.info("action=user_retrieve_success identifier_type={}", tipoIdentificador);
            return usuarioMapper.toDetalhesUsuario(entity);
        } catch (RuntimeException e) {
            if (e instanceof ApplicationException ex && ex.getErrorCode() == 404) {
                log.warn("action=user_retrieve_not_found identifier_type={}", tipoIdentificador);
            }
            throw new ApplicationException(404, MENSAGEM_NAO_ENCONTRADO);
        }
    }

    private void validarPayloads(Usuario usuario) {
        log.info("Validating user payload: {}", usuario.nomeUsuario());
        usuarioPayload.validarUsuario(usuario);
    }

    private void validarEmailDisponivel(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new ApplicationException(409, "Já existe um usuário cadastrado com este e-mail");
        }
    }

    private void validarNomeUsuarioDisponivel(String nomeUsuario) {
        if (usuarioRepository.existsByNomeUsuario(nomeUsuario)) {
            throw new ApplicationException(409, "Já existe um usuário cadastrado com este nome de usuário");
        }
    }
}
