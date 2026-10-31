package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteUseCasePortIn;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes")
@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
public class ClienteController implements ClienteAPI {

    private final ClienteUseCasePortIn useCase;

    public ClienteController(ClienteUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<ClienteResponse> criar(ClienteRequest request) {
        return Criado.em(useCase.criar(request), ClienteResponse::id);
    }

    @Override
    public ResponseEntity<Pagina<ClienteResponse>> listar(String documento, Integer pagina, Integer tamanho) {
        return ResponseEntity.ok(useCase.listar(documento, pagina, tamanho));
    }

    @Override
    public ResponseEntity<ClienteResponse> buscarPorId(Long id) {
        return ResponseEntity.ok(useCase.buscarPorId(id));
    }

    @Override
    public ResponseEntity<ClienteResponse> atualizar(Long id, ClienteRequest request) {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')") // funcionário não exclui cadastros
    public ResponseEntity<Void> desativar(Long id) {
        useCase.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
