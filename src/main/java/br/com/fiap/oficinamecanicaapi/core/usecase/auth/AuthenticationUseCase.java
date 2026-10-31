package br.com.fiap.oficinamecanicaapi.core.usecase.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthenticationUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.CadastroRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.NaoAutenticadoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficinamecanicaapi.shared.utils.MascaraDados;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthenticationUseCase implements AuthenticationUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationUseCase.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private volatile String hashFicticio;

    private final UsuarioUseCasePortIn usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProviderUseCase jwtTokenProvider;
    private final MeterRegistry meterRegistry;
    private final LimiteTentativasLogin limiteTentativas;

    public AuthenticationUseCase(UsuarioUseCasePortIn usuarioService,
                                 PasswordEncoder passwordEncoder,
                                 JwtTokenProviderUseCase jwtTokenProvider,
                                 LimiteTentativasLogin limiteTentativas,
                                 MeterRegistry meterRegistry) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.meterRegistry = meterRegistry;
        this.limiteTentativas = limiteTentativas;
    }

    /**
     * Usuário gravado numa transação (ADR 003).
     */
    @Override
    @Transactional
    public void cadastrarUsuario(CadastroRequest request) {
        log.info("action=register_start username={}", request.nomeUsuario());

        usuarioService.cadastrar(new Usuario(request.nome(), request.email(), request.nomeUsuario(),
                request.senha(), request.trocaSenhaObrigatoria(), request.perfil(), request.clienteId()));
        meterRegistry.counter("oficina.auth.cadastro", "perfil", request.perfil().name()).increment();

        log.info("action=register_success username={}", request.nomeUsuario());
    }

    @Override
    public AuthToken autenticar(SigninRequest request) {
        log.info("action=signin_start identifier={}", MascaraDados.mascararIdentificador(request.identificador()));
        limiteTentativas.verificarBloqueio(request.identificador());

        try {
            var detalhesUsuario = usuarioService.buscarPorIdentificador(request.identificador());

            if (!passwordEncoder.matches(request.senha(), detalhesUsuario.usuario().senha())) {
                contarLogin("senha_invalida");
                log.warn("action=signin_invalid_password identifier={}", MascaraDados.mascararIdentificador(request.identificador()));
                limiteTentativas.registrarFalha(request.identificador());
                throw new NaoAutenticadoException("Credenciais inválidas");
            }

            String token = BEARER_PREFIX + jwtTokenProvider.gerarToken(detalhesUsuario);
            contarLogin("sucesso");
            limiteTentativas.registrarSucesso(request.identificador());
            log.info("action=signin_success identifier={} changeRequired={}",
                    MascaraDados.mascararIdentificador(request.identificador()), detalhesUsuario.usuario().trocaSenhaObrigatoria());

            return AuthToken.builder()
                    .token(token)
                    .trocaSenhaObrigatoria(detalhesUsuario.usuario().trocaSenhaObrigatoria())
                    .build();
        } catch (RecursoNaoEncontradoException e) {
            // Usuário inexistente: mesma resposta e mesmo custo de bcrypt da senha errada, para nem o status
            // nem o tempo revelarem quais logins existem. Usa matches, e não encode: o bcrypt recusa no encode
            // senha com mais de 72 bytes, e o 500 resultante voltaria a revelar que o usuário não existe.
            passwordEncoder.matches(request.senha(), hashFicticio());
            log.warn("action=signin_user_not_found identifier={}", MascaraDados.mascararIdentificador(request.identificador()));
            contarLogin("usuario_inexistente");
            limiteTentativas.registrarFalha(request.identificador());
            throw new NaoAutenticadoException("Credenciais inválidas");
        }
    }

    @Override
    public Boolean validarToken(String token) {
        log.debug("action=jwt_validate_start");

        String rawToken = token != null && token.startsWith(BEARER_PREFIX)
                ? token.substring(BEARER_PREFIX.length())
                : token;

        try {
            boolean valido = jwtTokenProvider.validarToken(rawToken);
            if (valido) {
                log.debug("action=jwt_validate_success");
            } else {
                log.warn("action=jwt_validate_invalid");
            }
            return valido;
        } catch (NaoAutenticadoException e) {
            log.warn("action=jwt_validate_error error={}", e.getMessage());
            throw e;
        }
    }

    private void contarLogin(String resultado) {
        meterRegistry.counter("oficina.auth.login", "resultado", resultado).increment();
    }

    /**
     * Hash comparado quando o usuário não existe, para o login custar o mesmo bcrypt. A senha é aleatória, gerada
     * uma vez: ninguém a conhece e não há literal de senha no código (o Sonar trata literal no encode como vazada).
     */
    private String hashFicticio() {
        if (hashFicticio == null) {
            hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
        }
        return hashFicticio;
    }
}
