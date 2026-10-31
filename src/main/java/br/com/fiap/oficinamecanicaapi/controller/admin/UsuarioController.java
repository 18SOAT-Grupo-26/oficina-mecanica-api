package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthenticationUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.CadastroRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cadastro de usuários. Fica fora de /auth, que só tem as rotas públicas de login e validação do token.
 */
@RestController
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController implements UsuarioAPI {

    private final AuthenticationUseCasePortIn authService;

    public UsuarioController(AuthenticationUseCasePortIn authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<Void> cadastrar(CadastroRequest request) {
        authService.cadastrarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
