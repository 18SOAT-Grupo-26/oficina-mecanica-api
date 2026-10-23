package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecaInsumo.PecaInsumoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/insumos")
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMINISTRATOR')")
public class PecaInsumoController implements PecaInsumoAPI {
    private final PecaInsumoUseCasePortIn useCase;

    public PecaInsumoController(PecaInsumoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    @PostMapping
    public ResponseEntity<PecaInsumoResponse> criar(@RequestBody PecaInsumoRequest request) throws ApplicationException {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criar(request));
    }

    @Override
    @GetMapping
    public ResponseEntity<Pagina<PecaInsumoResponse>> listar(@RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(useCase.listar(page, size));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<PecaInsumoResponse> buscarPorId(@PathVariable Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.buscarPorId(id));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<PecaInsumoResponse> atualizar(@PathVariable Long id, @RequestBody PecaInsumoRequest request) throws ApplicationException {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) throws ApplicationException {
        useCase.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
