package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoUseCasePortIn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/veiculos")
@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
public class VeiculoController implements VeiculoAPI {

    private final VeiculoUseCasePortIn useCase;

    public VeiculoController(VeiculoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<VeiculoResponse> criar(VeiculoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criar(request));
    }

    @Override
    public ResponseEntity<Pagina<VeiculoResponse>> listar(Long clienteId, Integer page, Integer size) {
        return ResponseEntity.ok(useCase.listar(clienteId, page, size));
    }

    @Override
    public ResponseEntity<VeiculoResponse> buscarPorId(Long id) {
        return ResponseEntity.ok(useCase.buscarPorId(id));
    }

    @Override
    public ResponseEntity<VeiculoResponse> buscarPorPlaca(String placa) {
        return ResponseEntity.ok(useCase.buscarPorPlaca(placa));
    }

    @Override
    public ResponseEntity<VeiculoResponse> atualizar(Long id, VeiculoRequest request) {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')") // funcionário não exclui cadastros
    public ResponseEntity<Void> desativar(Long id) {
        useCase.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
