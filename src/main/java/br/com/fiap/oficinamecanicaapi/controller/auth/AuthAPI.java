package br.com.fiap.oficinamecanicaapi.controller.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.RegisterRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
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

    @Operation(summary = "Register User", tags = "Authentication")
    @ApiResponse(responseCode = "201", description = "User registered successfully", content = @Content)
    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping("/register")
    ResponseEntity<Void> registerUser(@RequestBody @Valid RegisterRequest registerRequest) throws ApplicationException;

    @Operation(summary = "Generate Token", tags = "Authentication")
    @ApiResponse(responseCode = "201", description = "Token generated successfully", content = @Content)
    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping("/login")
    ResponseEntity<AuthToken> generateToken(@RequestBody @Valid SigninRequest request) throws ApplicationException;

    @Operation(summary = "Validate Token", tags = "Authentication")
    @ApiResponse(responseCode = "200", description = "Token is valid", content = @Content)
    @ApiResponse(responseCode = "404", description = "Token not found", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/validate")
    ResponseEntity<Boolean> validateToken(@RequestHeader(value = "Authorization") String token) throws ApplicationException;

}
