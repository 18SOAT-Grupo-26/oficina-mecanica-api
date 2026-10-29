package br.com.fiap.oficinamecanicaapi.core.usecase.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.usuario.UsuarioPortOut;
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

    private final UsuarioPortOut usuarioPortOut;
    private final UsuarioPayload usuarioPayload;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaSenha politicaSenha;
    private final ClientePortOut clientePortOut;

    public UsuarioUseCase(UsuarioPortOut usuarioPortOut,
                          UsuarioPayload usuarioPayload,
                          PasswordEncoder passwordEncoder,
                          PoliticaSenha politicaSenha,
                          ClientePortOut clientePortOut) {
        this.usuarioPortOut = usuarioPortOut;
        this.usuarioPayload = usuarioPayload;
        this.passwordEncoder = passwordEncoder;
        this.politicaSenha = politicaSenha;
        this.clientePortOut = clientePortOut;
    }

    @SuppressWarnings("java:S2139")
    @Override
    @Transactional
    public void cadastrar(Usuario usuario) {
        try {
            validarPayloads(usuario);
            politicaSenha.validar(usuario.senha());
            validarEmailDisponivel(usuario.email());
            validarNomeUsuarioDisponivel(usuario.nomeUsuario());
            validarCliente(usuario);

            var usuarioComSenhaCifrada = new Usuario(
                    usuario.nome(),
                    usuario.email(),
                    usuario.nomeUsuario(),
                    passwordEncoder.encode(usuario.senha()),
                    usuario.trocaSenhaObrigatoria(),
                    usuario.perfil(),
                    usuario.clienteId()
            );

            usuarioPortOut.cadastrar(usuarioComSenhaCifrada);
            log.info("action=user_register_success username={}", usuario.nomeUsuario());
        } catch (ApplicationException e) {
            log.warn("action=user_register_rejected username={} status={} motivo={}",
                    usuario.nomeUsuario(), e.getErrorCode(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.error("action=user_register_failure username={}", usuario.nomeUsuario(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeAdministrador() {
        return usuarioPortOut.existePorPerfil(Perfil.ADMIN);
    }

    @Override
    @Transactional(readOnly = true)
    public DetalhesUsuario buscarPorIdentificador(String identificador) {
        var tipoIdentificador = usuarioPayload.isEmail(identificador) ? "email" : "username";
        try {
            var detalhes = (usuarioPayload.isEmail(identificador)
                    ? usuarioPortOut.buscarPorEmail(identificador)
                    : usuarioPortOut.buscarPorNomeUsuario(identificador))
                    .orElseThrow(() -> new ApplicationException(404, MENSAGEM_NAO_ENCONTRADO));
            log.debug("action=user_retrieve_success identifier_type={}", tipoIdentificador);
            return detalhes;
        } catch (RuntimeException e) {
            if (e instanceof ApplicationException ex && ex.getErrorCode() == 404) {
                log.warn("action=user_retrieve_not_found identifier_type={}", tipoIdentificador);
            }
            throw new ApplicationException(404, MENSAGEM_NAO_ENCONTRADO);
        }
    }

    private void validarPayloads(Usuario usuario) {
        log.debug("Validating user payload: {}", usuario.nomeUsuario());
        usuarioPayload.validarUsuario(usuario);
    }

    private void validarEmailDisponivel(String email) {
        if (usuarioPortOut.existePorEmail(email)) {
            throw new ApplicationException(409, "Já existe um usuário cadastrado com este e-mail");
        }
    }

    /**
     * O usuário CLIENTE só enxerga as OS do cliente ligado a ele; os outros perfis não têm cliente.
     */
    private void validarCliente(Usuario usuario) {
        if (usuario.perfil() != Perfil.CLIENTE) {
            if (usuario.clienteId() != null) {
                throw new ApplicationException(400, "Só o perfil CLIENTE é ligado a um cliente");
            }
            return;
        }
        if (usuario.clienteId() == null) {
            throw new ApplicationException(400, "Informe o clienteId para o perfil CLIENTE");
        }
        if (clientePortOut.buscarAtivoPorId(usuario.clienteId()).isEmpty()) {
            throw new ApplicationException(404, "Cliente não encontrado");
        }
        if (usuarioPortOut.existePorClienteId(usuario.clienteId())) {
            throw new ApplicationException(409, "Já existe um usuário para este cliente");
        }
    }

    private void validarNomeUsuarioDisponivel(String nomeUsuario) {
        if (usuarioPortOut.existePorNomeUsuario(nomeUsuario)) {
            throw new ApplicationException(409, "Já existe um usuário cadastrado com este nome de usuário");
        }
    }
}
