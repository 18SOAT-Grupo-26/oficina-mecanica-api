package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public interface ClienteAPI {

    @Operation(summary = "Cadastrar cliente", tags = "Cliente")
    @ApiResponse(responseCode = "201", description = "Cliente cadastrado", content = @Content(schema = @Schema(implementation = ClienteResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos (inclui CPF/CNPJ inválido)", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Documento já cadastrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping
    ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) throws ApplicationException;

    @Operation(summary = "Listar clientes ativos", tags = "Cliente")
    @ApiResponse(responseCode = "200", description = "Página de clientes ativos")
    @GetMapping
    ResponseEntity<Pagina<ClienteResponse>> listar(@Parameter(description = "Página, a partir de 1 (padrão: 1)") @RequestParam(required = false) Integer page,
                                                   @Parameter(description = "Itens por página (padrão: 20, máximo: 100)") @RequestParam(required = false) Integer size);

    @Operation(summary = "Buscar cliente por id", tags = "Cliente")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(schema = @Schema(implementation = ClienteResponse.class)))
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/{id}")
    ResponseEntity<ClienteResponse> buscarPorId(@PathVariable Long id) throws ApplicationException;

    @Operation(summary = "Buscar cliente por CPF ou CNPJ", tags = "Cliente")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(schema = @Schema(implementation = ClienteResponse.class)))
    @ApiResponse(responseCode = "400", description = "Documento inválido", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/documento/{documento}")
    ResponseEntity<ClienteResponse> buscarPorDocumento(@PathVariable String documento) throws ApplicationException;

    @Operation(summary = "Atualizar cliente", tags = "Cliente")
    @ApiResponse(responseCode = "200", description = "Cliente atualizado", content = @Content(schema = @Schema(implementation = ClienteResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Documento já cadastrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PutMapping("/{id}")
    ResponseEntity<ClienteResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) throws ApplicationException;

    @Operation(summary = "Remover cliente (soft delete)", tags = "Cliente")
    @ApiResponse(responseCode = "204", description = "Cliente desativado")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> desativar(@PathVariable Long id) throws ApplicationException;
}
