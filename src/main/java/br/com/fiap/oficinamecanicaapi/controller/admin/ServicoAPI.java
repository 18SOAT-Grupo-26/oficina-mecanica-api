package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

public interface ServicoAPI {

    @Operation(summary = "Create service", tags = "Service")
    @ApiResponse(responseCode = "201", description = "Service created successfully", content = @Content(schema = @Schema(implementation = ServicoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Service with the same name already exists", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    ResponseEntity<ServicoResponse> criar(@RequestHeader(value = "Authorization") String token, @RequestBody ServicoRequest request) throws ApplicationException;

    @Operation(summary = "Listar services", tags = "Service")
    @ApiResponse(responseCode = "200", description = "Página de services")
    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    ResponseEntity<Pagina<ServicoResponse>> listar(@RequestHeader(value = "Authorization") String token, @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) throws ApplicationException;

    @Operation(summary = "Buscar service por id", tags = "Service")
    @ApiResponse(responseCode = "200", description = "Service encontrado", content = @Content(schema = @Schema(implementation = ServicoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Service não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    ResponseEntity<ServicoResponse> buscarPorId(@RequestHeader(value = "Authorization") String token, @PathVariable Long id) throws ApplicationException;

    @Operation(summary = "Atualizar service", tags = "Service")
    @ApiResponse(responseCode = "200", description = "Service atualizado", content = @Content(schema = @Schema(implementation = ServicoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Service não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Nome de service já cadastrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    ResponseEntity<ServicoResponse> atualizar(@RequestHeader(value = "Authorization") String token, @PathVariable Long id, @RequestBody ServicoRequest request) throws ApplicationException;

    @Operation(summary = "Excluir service", tags = "Service")
    @ApiResponse(responseCode = "204", description = "Service excluído")
    @ApiResponse(responseCode = "404", description = "Service não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    ResponseEntity<Void> excluir(@RequestHeader(value = "Authorization") String token,@PathVariable Long id) throws ApplicationException;
}
