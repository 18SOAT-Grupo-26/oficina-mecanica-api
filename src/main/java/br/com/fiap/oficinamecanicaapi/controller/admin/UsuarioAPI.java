package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.port.in.auth.CadastroRequest;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface UsuarioAPI {

    @Operation(summary = "Cadastrar usuário (somente ADMIN)", tags = "Usuário")
    @ApiResponse(responseCode = "201", description = "Usuário cadastrado", content = @Content)
    @ApiResponse(responseCode = "400", description = "Dados inválidos (inclui senha fora da política e clienteId ausente ou indevido para o perfil)", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Cliente do perfil CLIENTE não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "E-mail ou nome de usuário já cadastrado, ou cliente que já tem usuário", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping
    ResponseEntity<Void> cadastrar(@RequestBody @Valid CadastroRequest request);
}
