package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.VeiculoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.VeiculoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/veiculos")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class VeiculoController implements VeiculoAPI {

    private final VeiculoUseCasePortIn useCase;

    public VeiculoController(VeiculoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<VeiculoResponse> criar(VeiculoRequest request) throws ApplicationException {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criar(request));
    }

    @Override
    public ResponseEntity<Pagina<VeiculoResponse>> listar(String token, Long clienteId, Integer page, Integer size) {
        return ResponseEntity.ok(useCase.listar(clienteId, page, size));
    }

    @Override
    public ResponseEntity<VeiculoResponse> buscarPorId(String token, Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.buscarPorId(id));
    }

    @Override
    public ResponseEntity<VeiculoResponse> buscarPorPlaca(String token, String placa) throws ApplicationException {
        return ResponseEntity.ok(useCase.buscarPorPlaca(placa));
    }

    @Override
    public ResponseEntity<VeiculoResponse> atualizar(String token, Long id, VeiculoRequest request) throws ApplicationException {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    public ResponseEntity<Void> desativar(String token, Long id) throws ApplicationException {
        useCase.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
