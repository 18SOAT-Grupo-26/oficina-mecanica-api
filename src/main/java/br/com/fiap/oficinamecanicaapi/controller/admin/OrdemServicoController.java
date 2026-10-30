package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.TempoMedioExecucaoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas dos funcionários e do admin. O cliente acompanha as próprias OS pelo {@code MinhasOrdensController}.
 */
@RestController
@RequestMapping("/ordens-servico")
@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
public class OrdemServicoController implements OrdemServicoAPI {

    private final OrdemServicoUseCasePortIn useCase;

    public OrdemServicoController(OrdemServicoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> criar(OrdemServicoRequest request) throws ApplicationException {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criar(request));
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> consultar(Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.consultar(id));
    }

    @Override
    public ResponseEntity<Pagina<OrdemServicoResponse>> listar(Long clienteId, StatusOrdemServico status,
                                                               Integer pagina, Integer tamanho) {
        return ResponseEntity.ok(useCase.listar(clienteId, status, pagina, tamanho));
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> atualizar(Long id, OrdemServicoRequest request) throws ApplicationException {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> alterarStatus(Long id, OrdemServicoRequest.Status request) throws ApplicationException {
        return ResponseEntity.ok(useCase.alterarStatus(id, request));
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> gerarOrcamento(Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.registrarOrcamentoEnviado(id));
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> aprovarOrcamento(Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.registrarOrcamentoAprovado(id));
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> rejeitarOrcamento(Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.registrarOrcamentoRejeitado(id));
    }

    @Override
    public ResponseEntity<TempoMedioExecucaoResponse> consultarTempoMedioExecucao() {
        return ResponseEntity.ok(useCase.consultarTempoMedioExecucao());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')") // funcionário não exclui cadastros
    public ResponseEntity<Void> cancelar(Long id) throws ApplicationException {
        useCase.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
