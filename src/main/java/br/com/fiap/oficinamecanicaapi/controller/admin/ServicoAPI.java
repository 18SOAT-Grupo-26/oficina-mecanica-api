package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public interface ServicoAPI {

    @Operation(summary = "Criar serviço", tags = "Serviço")
    @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso", content = @Content(schema = @Schema(implementation = ServicoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Serviço com o mesmo nome já existe", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Body ausente ou ilegível", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping
    ResponseEntity<ServicoResponse> criar(@RequestBody ServicoRequest request);

    @Operation(summary = "Listar serviços", tags = "Serviço")
    @ApiResponse(responseCode = "200", description = "Página de serviços")
    @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping
    ResponseEntity<Pagina<ServicoResponse>> listar(@Parameter(description = "Página, a partir de 1 (padrão: 1)") @RequestParam(required = false) Integer pagina, @Parameter(description = "Itens por página (padrão: 20, máximo: 100)") @RequestParam(required = false) Integer tamanho);

    @Operation(summary = "Buscar serviço por id", tags = "Serviço")
    @ApiResponse(responseCode = "200", description = "Serviço encontrado", content = @Content(schema = @Schema(implementation = ServicoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Serviço não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/{id}")
    ResponseEntity<ServicoResponse> buscarPorId(@PathVariable Long id);

    @Operation(summary = "Atualizar serviço", tags = "Serviço")
    @ApiResponse(responseCode = "200", description = "Service atualizado", content = @Content(schema = @Schema(implementation = ServicoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Service não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Nome de service já cadastrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PutMapping("/{id}")
    ResponseEntity<ServicoResponse> atualizar(@PathVariable Long id, @RequestBody ServicoRequest request);

    @Operation(summary = "Remover serviço (soft delete)", tags = "Serviço")
    @ApiResponse(responseCode = "204", description = "Serviço desativado; as OS que já o usam não mudam")
    @ApiResponse(responseCode = "404", description = "Serviço não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> desativar(@PathVariable Long id);
}
