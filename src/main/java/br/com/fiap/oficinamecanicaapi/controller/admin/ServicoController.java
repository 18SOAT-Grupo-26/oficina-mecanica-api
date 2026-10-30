package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/servico")
@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
public class ServicoController implements ServicoAPI {

    private final ServicoUseCasePortIn useCase;

    public ServicoController(ServicoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<ServicoResponse> criar(ServicoRequest request) throws ApplicationException {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criar(request));
    }

    @Override
    public ResponseEntity<Pagina<ServicoResponse>> listar(Integer pagina, Integer tamanho) {
        return ResponseEntity.ok(useCase.listar(pagina, tamanho));
    }

    @Override
    public ResponseEntity<ServicoResponse> buscarPorId(Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.buscarPorId(id));
    }

    @Override
    public ResponseEntity<ServicoResponse> atualizar(Long id, ServicoRequest request) throws ApplicationException {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(Long id) throws ApplicationException {
        useCase.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
