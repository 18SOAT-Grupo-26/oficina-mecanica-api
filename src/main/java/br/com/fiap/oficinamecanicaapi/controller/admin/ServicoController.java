package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoUseCasePortIn;
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
    public ResponseEntity<ServicoResponse> criar(ServicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criar(request));
    }

    @Override
    public ResponseEntity<Pagina<ServicoResponse>> listar(Integer page, Integer size) {
        return ResponseEntity.ok(useCase.listar(page, size));
    }

    @Override
    public ResponseEntity<ServicoResponse> buscarPorId(Long id) {
        return ResponseEntity.ok(useCase.buscarPorId(id));
    }

    @Override
    public ResponseEntity<ServicoResponse> atualizar(Long id, ServicoRequest request) {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(Long id) {
        useCase.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
