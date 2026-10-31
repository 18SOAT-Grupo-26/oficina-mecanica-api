package br.com.fiap.oficinamecanicaapi.controller.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.exception.model.ErrorTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Acompanhamento da OS pelo cliente (perfil CLIENTE). O cliente vem do token, nunca da URL: ele só enxerga as
 * próprias OS.
 */
public interface MinhasOrdensAPI {

    @Operation(summary = "Listar minhas OS", description = "OS do cliente logado, com status, itens e histórico.",
            tags = "Acompanhamento da OS")
    @ApiResponse(responseCode = "200", description = "OS do cliente", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "403", description = "Perfil diferente de CLIENTE", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping
    ResponseEntity<List<OrdemServicoResponse>> listar(@Parameter(hidden = true) @AuthenticationPrincipal DetalhesUsuario usuario)
            throws ApplicationException;

    @Operation(summary = "Consultar minha OS", description = "Status, itens e histórico. A OS de outro cliente responde 404.",
            tags = "Acompanhamento da OS")
    @ApiResponse(responseCode = "200", description = "OS encontrada", content = @Content(schema = @Schema(implementation = OrdemServicoResponse.class)))
    @ApiResponse(responseCode = "403", description = "Perfil diferente de CLIENTE", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @ApiResponse(responseCode = "404", description = "OS não encontrada ou de outro cliente", content = @Content(schema = @Schema(implementation = ErrorTemplate.class)))
    @GetMapping("/{id}")
    ResponseEntity<OrdemServicoResponse> consultar(@Parameter(hidden = true) @AuthenticationPrincipal DetalhesUsuario usuario,
                                                   @PathVariable Long id) throws ApplicationException;
}
