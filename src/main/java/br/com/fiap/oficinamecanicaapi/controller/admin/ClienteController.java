package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class ClienteController implements ClienteAPI {

    private final ClienteUseCasePortIn useCase;

    public ClienteController(ClienteUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) throws ApplicationException {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criar(request));
    }

    @Override
    public ResponseEntity<Pagina<ClienteResponse>> listar(@RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(useCase.listar(page, size));
    }

    @Override
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.buscarPorId(id));
    }

    @Override
    public ResponseEntity<ClienteResponse> buscarPorDocumento(@PathVariable String documento) throws ApplicationException {
        return ResponseEntity.ok(useCase.buscarPorDocumento(documento));
    }

    @Override
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody ClienteRequest request) throws ApplicationException {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    public ResponseEntity<Void> desativar(@PathVariable Long id) throws ApplicationException {
        useCase.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
