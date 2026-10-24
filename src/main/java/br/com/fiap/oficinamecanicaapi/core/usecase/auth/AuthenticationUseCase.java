package br.com.fiap.oficinamecanicaapi.core.usecase.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.RegisterRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthenticationUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.identity.IdentityUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.user.UserUseCasePortIn;
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

    private final IdentityUseCasePortIn identityService;
    private final UserUseCasePortIn userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProviderUseCase jwtTokenProvider;

    public AuthenticationUseCase(IdentityUseCasePortIn identityService,
                                 UserUseCasePortIn userService,
                                 PasswordEncoder passwordEncoder,
                                 JwtTokenProviderUseCase jwtTokenProvider,
                                 MeterRegistry meterRegistry) {
        this.identityService = identityService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /** Identidade, contato, endereço e usuário numa transação só (ADR 003). */
    @Override
    @Transactional
    public void registerUser(RegisterRequest registerRequest) throws ApplicationException {
        try {
            log.info("action=register_start username={}", registerRequest.user().username());
            var identityEntity = identityService.registerIdentity(registerRequest.identity());
            userService.registerUser(registerRequest.user(), identityEntity);
            log.info("action=register_success username={}", registerRequest.user().username());
        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(500, "Error occurred while registering user");
        }
    }

    @Override
    public AuthToken authenticate(SigninRequest request) throws ApplicationException {
        log.info("action=signin_start identifier={}", request.identifier());
        var userDetails = userService.retrieveUser(request);

        if (!passwordEncoder.matches(request.password(), userDetails.user().password())) {
            log.warn("action=signin_invalid_password identifier={}", request.identifier());
            throw new ApplicationException(401, "Invalid credentials");
        }

        String token = BEARER_PREFIX + jwtTokenProvider.generateToken(userDetails);
        log.info("action=signin_success identifier={} changeRequired={}",
                request.identifier(), userDetails.user().changeRequired());

        return AuthToken.builder()
                .token(token)
                .changeRequired(userDetails.user().changeRequired())
                .build();
    }

    @Override
    public Boolean validateJwtToken(String token) throws ApplicationException {
        log.debug("action=jwt_validate_start");
        String rawToken = token != null && token.startsWith(BEARER_PREFIX)
                ? token.substring(BEARER_PREFIX.length())
                : token;
        try {
            boolean valid = jwtTokenProvider.validateToken(rawToken);
            if (valid) {
                log.debug("action=jwt_validate_success");
            } else {
                log.warn("action=jwt_validate_invalid");
            }
            return valid;
        } catch (ApplicationException e) {
            log.warn("action=jwt_validate_error error={}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("action=jwt_validate_error error={}", e.getMessage());
            throw new ApplicationException(500, "Error occurred while validating JWT token");
        }
    }

}
