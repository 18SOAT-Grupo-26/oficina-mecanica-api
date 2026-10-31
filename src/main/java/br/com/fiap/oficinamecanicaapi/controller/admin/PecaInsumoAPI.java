package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public interface PecaInsumoAPI {

    @Operation(summary = "Create Insumo", tags = "Insumo")
    @ApiResponse(responseCode = "201", description = "Insumo created successfully", content = @Content(schema = @Schema(implementation = PecaInsumoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Insumo with the same name already exists", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Body ausente ou ilegível", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping
    ResponseEntity<PecaInsumoResponse> criar(@RequestBody PecaInsumoRequest request);

    @Operation(summary = "Listar insumos", tags = "Insumo")
    @ApiResponse(responseCode = "200", description = "Página de insumos")
    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Insumo with the same name already exists", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping
    ResponseEntity<Pagina<PecaInsumoResponse>> listar(@Parameter(description = "Página, a partir de 1 (padrão: 1)") @RequestParam(required = false) Integer pagina,
                                                      @Parameter(description = "Itens por página (padrão: 20, máximo: 100)") @RequestParam(required = false) Integer tamanho);

    @Operation(summary = "Buscar insumo por id", tags = "Insumo")
    @ApiResponse(responseCode = "200", description = "Insumo encontrado", content = @Content(schema = @Schema(implementation = PecaInsumoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Insumo não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/{id}")
    ResponseEntity<PecaInsumoResponse> buscarPorId(@PathVariable Long id);


    @Operation(summary = "Atualizar insumo", tags = "Insumo")
    @ApiResponse(responseCode = "200", description = "Insumo atualizado", content = @Content(schema = @Schema(implementation = PecaInsumoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Insumo não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Nome de insumo já cadastrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PutMapping("/{id}")
    ResponseEntity<PecaInsumoResponse> atualizar(@PathVariable Long id, @RequestBody PecaInsumoRequest request);

    @Operation(summary = "Remover insumo (soft delete)", tags = "Insumo")
    @ApiResponse(responseCode = "204", description = "Insumo desativado; as OS que já o usam não mudam")
    @ApiResponse(responseCode = "404", description = "Insumo não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "O insumo tem unidades reservadas para orçamentos enviados e não pode ser desativado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> desativar(@PathVariable Long id);
}
