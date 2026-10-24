package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface PecaInsumoAPI {

    @Operation(summary = "Create Insumo", tags = "Insumo")
    @ApiResponse(responseCode = "201", description = "Insumo created successfully", content = @Content(schema = @Schema(implementation = PecaInsumoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Insumo with the same name already exists", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    ResponseEntity<PecaInsumoResponse> criar(PecaInsumoRequest request) throws ApplicationException;

    @Operation(summary = "Listar insumos", tags = "Insumo")
    @ApiResponse(responseCode = "200", description = "Página de insumos")
    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Insumo with the same name already exists", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "422", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    ResponseEntity<Pagina<PecaInsumoResponse>> listar(Integer page, Integer size) throws ApplicationException;

    @Operation(summary = "Buscar insumo por id", tags = "Insumo")
    @ApiResponse(responseCode = "200", description = "Insumo encontrado", content = @Content(schema = @Schema(implementation = PecaInsumoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Insumo não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    ResponseEntity<PecaInsumoResponse> buscarPorId(Long id) throws ApplicationException;


    @Operation(summary = "Atualizar insumo", tags = "Insumo")
    @ApiResponse(responseCode = "200", description = "Insumo atualizado", content = @Content(schema = @Schema(implementation = PecaInsumoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Insumo não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Nome de insumo já cadastrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    ResponseEntity<PecaInsumoResponse> atualizar(Long id, PecaInsumoRequest request) throws ApplicationException;

    @Operation(summary = "Excluir insumo", tags = "Insumo")
    @ApiResponse(responseCode = "204", description = "Insumo excluído")
    @ApiResponse(responseCode = "404", description = "Insumo não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    ResponseEntity<Void> excluir(Long id) throws ApplicationException;
}
