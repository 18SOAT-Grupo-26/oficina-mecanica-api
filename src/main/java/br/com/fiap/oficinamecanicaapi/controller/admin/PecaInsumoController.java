package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo.PecaInsumoUseCasePortIn;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pecas-insumos")
@PreAuthorize("hasRole('FUNCIONARIO') or hasRole('ADMIN')")
public class PecaInsumoController implements PecaInsumoAPI {
    private final PecaInsumoUseCasePortIn useCase;

    public PecaInsumoController(PecaInsumoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<PecaInsumoResponse> criar(PecaInsumoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criar(request));
    }

    @Override
    public ResponseEntity<Pagina<PecaInsumoResponse>> listar(Integer pagina, Integer tamanho) {
        return ResponseEntity.ok(useCase.listar(pagina, tamanho));
    }

    @Override
    public ResponseEntity<PecaInsumoResponse> buscarPorId(Long id) {
        return ResponseEntity.ok(useCase.buscarPorId(id));
    }

    @Override
    public ResponseEntity<PecaInsumoResponse> atualizar(Long id, PecaInsumoRequest request) {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(Long id) {
        useCase.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
