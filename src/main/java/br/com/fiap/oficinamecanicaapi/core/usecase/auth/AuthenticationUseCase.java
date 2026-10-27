package br.com.fiap.oficinamecanicaapi.core.usecase.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.CadastroRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthenticationUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationUseCase implements AuthenticationUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationUseCase.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final UsuarioUseCasePortIn usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProviderUseCase jwtTokenProvider;

    public AuthenticationUseCase(UsuarioUseCasePortIn usuarioService,
                                 PasswordEncoder passwordEncoder,
                                 JwtTokenProviderUseCase jwtTokenProvider,
                                 MeterRegistry meterRegistry) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** Usuário gravado numa transação (ADR 003). */
    @Override
    @Transactional
    public void cadastrarUsuario(CadastroRequest request) throws ApplicationException {
        try {
            log.info("action=register_start username={}", request.nomeUsuario());

            usuarioService.cadastrar(new Usuario(request.nome(), request.email(), request.nomeUsuario(),
                    request.senha(), request.trocaSenhaObrigatoria(), request.perfil()));

            log.info("action=register_success username={}", request.nomeUsuario());
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(500, "Erro ao cadastrar o usuário");
        }
    }

    @Override
    public AuthToken autenticar(SigninRequest request) throws ApplicationException {
        log.info("action=signin_start identifier={}", request.identificador());

        var detalhesUsuario = usuarioService.buscarPorIdentificador(request.identificador());

        if (!passwordEncoder.matches(request.senha(), detalhesUsuario.usuario().senha())) {
            log.warn("action=signin_invalid_password identifier={}", request.identificador());
            throw new ApplicationException(401, "Credenciais inválidas");
        }

        String token = BEARER_PREFIX + jwtTokenProvider.gerarToken(detalhesUsuario);
        log.info("action=signin_success identifier={} changeRequired={}",
                request.identificador(), detalhesUsuario.usuario().trocaSenhaObrigatoria());

        return AuthToken.builder()
                .token(token)
                .trocaSenhaObrigatoria(detalhesUsuario.usuario().trocaSenhaObrigatoria())
                .build();
    }

    @Override
    public Boolean validarToken(String token) throws ApplicationException {
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
        } catch (ApplicationException e) {
            log.warn("action=jwt_validate_error error={}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("action=jwt_validate_error error={}", e.getMessage());
            throw new ApplicationException(500, "Erro ao validar o token JWT");
        }
    }
}
