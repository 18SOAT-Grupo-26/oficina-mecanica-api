package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoResponse;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public interface VeiculoAPI {

    @Operation(summary = "Cadastrar veículo", tags = "Veículo")
    @ApiResponse(responseCode = "201", description = "Veículo cadastrado", content = @Content(schema = @Schema(implementation = VeiculoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos (inclui placa ou ano inválido)", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Placa já cadastrada", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PostMapping
    ResponseEntity<VeiculoResponse> criar(@Valid @RequestBody VeiculoRequest request);

    @Operation(summary = "Listar veículos ativos, opcionalmente por cliente ou placa", tags = "Veículo")
    @ApiResponse(responseCode = "200", description = "Página de veículos ativos (com placa, no máximo um; vazia se não houver)")
    @ApiResponse(responseCode = "400", description = "Placa inválida", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping
    ResponseEntity<Pagina<VeiculoResponse>> listar(@RequestParam(required = false) Long clienteId,
                                                   @Parameter(description = "Placa antiga ou Mercosul, com ou sem hífen") @RequestParam(required = false) String placa,
                                                   @Parameter(description = "Página, a partir de 1 (padrão: 1)") @RequestParam(required = false) Integer pagina,
                                                   @Parameter(description = "Itens por página (padrão: 20, máximo: 100)") @RequestParam(required = false) Integer tamanho);

    @Operation(summary = "Buscar veículo por id", tags = "Veículo")
    @ApiResponse(responseCode = "200", description = "Veículo encontrado", content = @Content(schema = @Schema(implementation = VeiculoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/{id}")
    ResponseEntity<VeiculoResponse> buscarPorId(@PathVariable Long id);

    @Operation(summary = "Atualizar veículo", tags = "Veículo")
    @ApiResponse(responseCode = "200", description = "Veículo atualizado", content = @Content(schema = @Schema(implementation = VeiculoResponse.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "Veículo ou cliente não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "409", description = "Placa já cadastrada", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @PutMapping("/{id}")
    ResponseEntity<VeiculoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody VeiculoRequest request);

    @Operation(summary = "Remover veículo (soft delete)", tags = "Veículo")
    @ApiResponse(responseCode = "204", description = "Veículo desativado")
    @ApiResponse(responseCode = "404", description = "Veículo não encontrado", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @DeleteMapping("/{id}")
    ResponseEntity<Void> desativar(@PathVariable Long id);
}
