package br.com.fiap.oficinamecanicaapi.controller.auth;

import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthenticationUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.SigninRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController implements AuthAPI {

    private final AuthenticationUseCasePortIn authService;

    public AuthController(AuthenticationUseCasePortIn authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<AuthToken> gerarToken(SigninRequest request) {
        return ResponseEntity.ok(authService.autenticar(request));
    }

    @Override
    public ResponseEntity<Boolean> validarToken(String token) {
        return ResponseEntity.ok(authService.validarToken(token));
    }
}
