package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.TempoMedioExecucaoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Papéis provisórios: todos os perfis até o grupo definir quem pode cada mudança. */
@RestController
@RequestMapping("/ordens-servico")
@PreAuthorize("hasAnyRole('OPERATOR', 'MANAGER', 'ADMINISTRATOR')")
public class OrdemServicoController implements OrdemServicoAPI {

    private final OrdemServicoUseCasePortIn useCase;

    public OrdemServicoController(OrdemServicoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> criar(String token, OrdemServicoRequest request) throws ApplicationException {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criar(request));
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> consultar(String token, Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.consultar(id));
    }

    @Override
    public ResponseEntity<List<OrdemServicoResponse>> listar(String token) throws ApplicationException {
        return null;
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> atualizar(String token, Long id, OrdemServicoRequest request) throws ApplicationException {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> alterarStatus(String token, Long id, OrdemServicoRequest.Status request) throws ApplicationException {
        return ResponseEntity.ok(useCase.alterarStatus(id, request));
    }

    @Override
    public ResponseEntity<TempoMedioExecucaoResponse> consultarTempoMedioExecucao() {
        return ResponseEntity.ok(useCase.consultarTempoMedioExecucao());
    }
}
