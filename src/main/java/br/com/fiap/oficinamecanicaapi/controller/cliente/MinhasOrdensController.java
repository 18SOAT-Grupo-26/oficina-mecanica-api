package br.com.fiap.oficinamecanicaapi.controller.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/minhas-ordens")
@PreAuthorize("hasRole('CLIENTE')")
public class MinhasOrdensController implements MinhasOrdensAPI {

    private final OrdemServicoUseCasePortIn useCase;

    public MinhasOrdensController(OrdemServicoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<List<OrdemServicoResponse>> listar(DetalhesUsuario usuario) throws ApplicationException {
        return ResponseEntity.ok(useCase.listar(usuario.usuario().clienteId()));
    }

    @Override
    public ResponseEntity<OrdemServicoResponse> consultar(DetalhesUsuario usuario, Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.consultarDoCliente(id, usuario.usuario().clienteId()));
    }
}
