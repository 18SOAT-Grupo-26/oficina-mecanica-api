package br.com.fiap.oficinamecanicaapi.controller.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

public interface AcompanhamentoOsAPI {

    @Operation(summary = "Consultar OS", description = "Status, itens e histórico.", tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "200", description = "OS encontrada", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "404", description = "OS não encontrada", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/consultar/{id}")
    ResponseEntity<OrdemServicoResponse> consultar(@RequestHeader(value = "Authorization") String token, @PathVariable Long id) throws ApplicationException;

    @Operation(summary = "Listar OS", description = "Status, itens e histórico.", tags = "Ordem de Serviço")
    @ApiResponse(responseCode = "200", description = "OS encontradas", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "404", description = "Nenhuma OS encontrada", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/listar")
    ResponseEntity<List<OrdemServicoResponse>> listar(@RequestHeader(value = "Authorization") String token) throws ApplicationException;

}
