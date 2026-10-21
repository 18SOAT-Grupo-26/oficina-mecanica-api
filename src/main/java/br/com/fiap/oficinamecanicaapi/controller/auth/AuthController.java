package br.com.fiap.oficinamecanicaapi.controller.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.RegisterRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthenticationUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController implements AuthAPI {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationUseCasePortIn authService;

    public AuthController(AuthenticationUseCasePortIn authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<Void> registerUser(@RequestBody @Valid RegisterRequest registerRequest) throws ApplicationException {
        log.info("Registering user: {}", registerRequest.identity().fullName());
        authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<AuthToken> generateToken(@RequestBody @Valid SigninRequest request)
            throws ApplicationException {
        log.info("Generating token for: {}", request.identifier());
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @Override
    public ResponseEntity<Boolean> validateToken(@RequestHeader(value = "Authorization") String token) throws ApplicationException {
        log.info("Validating token: {}", token);
        return ResponseEntity.ok(authService.validateJwtToken(token));
    }

}
