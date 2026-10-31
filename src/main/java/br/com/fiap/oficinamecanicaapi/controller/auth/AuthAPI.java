package br.com.fiap.oficinamecanicaapi.controller.auth;

import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.CadastroRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.SigninRequest;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface AuthAPI {

    @Operation(summary = "Cadastrar usuário (somente ADMIN)", tags = "Autenticação")
    @ApiResponse(responseCode = "201", description = "Usuário cadastrado", content = @Content)
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "E-mail ou documento já cadastrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping("/cadastro")
    ResponseEntity<Void> cadastrarUsuario(@RequestBody @Valid CadastroRequest request) throws ApplicationException;

    @Operation(summary = "Gerar token", tags = "Autenticação")
    @ApiResponse(responseCode = "200", description = "Token gerado", content = @Content)
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping("/login")
    ResponseEntity<AuthToken> gerarToken(@RequestBody @Valid SigninRequest request) throws ApplicationException;

    @Operation(summary = "Validar token", tags = "Autenticação")
    @ApiResponse(responseCode = "200", description = "Token válido", content = @Content)
    @GetMapping("/validate")
    ResponseEntity<Boolean> validarToken(@RequestHeader(value = "Authorization") String token) throws ApplicationException;

}
