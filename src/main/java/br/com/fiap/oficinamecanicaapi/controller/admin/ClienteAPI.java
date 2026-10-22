package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

public interface ClienteAPI {

    @Operation(summary = "Cadastrar cliente", tags = "Cliente")
    @ApiResponse(responseCode = "201", description = "Cliente cadastrado", content = @Content(schema = @Schema(implementation = ClienteResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos (inclui CPF/CNPJ inválido)", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Usuário sem perfil ADMINISTRATOR", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Documento já cadastrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping
    ResponseEntity<ClienteResponse> criar(ClienteRequest request) throws ApplicationException;

    @Operation(summary = "Listar clientes ativos", tags = "Cliente")
    @ApiResponse(responseCode = "200", description = "Página de clientes ativos")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Usuário sem perfil ADMINISTRATOR", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping
    ResponseEntity<Pagina<ClienteResponse>> listar(Integer page, Integer size);

    @Operation(summary = "Buscar cliente por id", tags = "Cliente")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(schema = @Schema(implementation = ClienteResponse.class)))
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Usuário sem perfil ADMINISTRATOR", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/{id}")
    ResponseEntity<ClienteResponse> buscarPorId(Long id) throws ApplicationException;

    @Operation(summary = "Buscar cliente por CPF ou CNPJ", tags = "Cliente")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(schema = @Schema(implementation = ClienteResponse.class)))
    @ApiResponse(responseCode = "400", description = "Documento inválido", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Usuário sem perfil ADMINISTRATOR", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/documento/{documento}")
    ResponseEntity<ClienteResponse> buscarPorDocumento(String documento) throws ApplicationException;

    @Operation(summary = "Atualizar cliente", tags = "Cliente")
    @ApiResponse(responseCode = "200", description = "Cliente atualizado", content = @Content(schema = @Schema(implementation = ClienteResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Usuário sem perfil ADMINISTRATOR", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Documento já cadastrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PutMapping("/{id}")
    ResponseEntity<ClienteResponse> atualizar(Long id, ClienteRequest request) throws ApplicationException;

    @Operation(summary = "Remover cliente (soft delete)", tags = "Cliente")
    @ApiResponse(responseCode = "204", description = "Cliente desativado")
    @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    @ApiResponse(responseCode = "403", description = "Usuário sem perfil ADMINISTRATOR", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> desativar(Long id) throws ApplicationException;
}
