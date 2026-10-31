package br.com.fiap.oficinamecanicaapi.controller.auth;

import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.SigninRequest;
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

    @Operation(summary = "Gerar token", tags = "Autenticação")
    @ApiResponse(responseCode = "200", description = "Token gerado", content = @Content(schema = @Schema(implementation = AuthToken.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "429", description = "Muitas tentativas de login; tente de novo em alguns minutos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping("/login")
    ResponseEntity<AuthToken> gerarToken(@RequestBody @Valid SigninRequest request);

    @Operation(summary = "Validar token", tags = "Autenticação")
    @ApiResponse(responseCode = "200", description = "Token válido (sempre true; token inválido responde 401)", content = @Content(schema = @Schema(implementation = Boolean.class)))
    @ApiResponse(responseCode = "400", description = "Header Authorization ausente", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "401", description = "Token inválido ou expirado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/validacao")
    ResponseEntity<Boolean> validarToken(@RequestHeader(value = "Authorization") String token);

}
