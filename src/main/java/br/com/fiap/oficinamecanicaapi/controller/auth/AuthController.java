package br.com.fiap.oficinamecanicaapi.controller.auth;

import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthenticationUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.CadastroRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.SigninRequest;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cadastrarUsuario(CadastroRequest request) throws ApplicationException {
        authService.cadastrarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<AuthToken> gerarToken(SigninRequest request)
            throws ApplicationException {
        return ResponseEntity.ok(authService.autenticar(request));
    }

    @Override
    public ResponseEntity<Boolean> validarToken(String token) throws ApplicationException {
        return ResponseEntity.ok(authService.validarToken(token));
    }
}
