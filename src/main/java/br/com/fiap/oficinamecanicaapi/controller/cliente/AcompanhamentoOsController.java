package br.com.fiap.oficinamecanicaapi.controller.cliente;

import br.com.fiap.oficinamecanicaapi.controller.admin.OrdemServicoAPI;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Papéis provisórios: todos os perfis até o grupo definir quem pode cada mudança. */
@RestController
@RequestMapping("/ordens-servico")
@PreAuthorize("hasAnyRole('OPERATOR', 'MANAGER', 'ADMINISTRATOR')")
public class AcompanhamentoOsController implements AcompanhamentoOsAPI {

    private final OrdemServicoUseCasePortIn useCase;

    public AcompanhamentoOsController(OrdemServicoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> consultar(String token, Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.consultar(id));
    }

    @Override
    public ResponseEntity<List<OrdemServicoResponse>> listar(String token) throws ApplicationException {
        return ResponseEntity.ok(useCase.listar());
    }


}
