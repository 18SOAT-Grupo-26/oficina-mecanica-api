package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecaInsumo.PecaInsumoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/insumos")
@PreAuthorize("hasRole('MANAGER')")
public class PecaInsumoController implements PecaInsumoAPI {
    private static final Logger log = LoggerFactory.getLogger(PecaInsumoController.class);

    private final PecaInsumoUseCasePortIn useCase;

    public PecaInsumoController(PecaInsumoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<PecaInsumoResponse> criar(@RequestBody PecaInsumoRequest request) throws ApplicationException {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criar(request));
    }

    @Override
    public ResponseEntity<Pagina<PecaInsumoResponse>> listar(@RequestParam(required = false) Integer page,
                                                             @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(useCase.listar(page, size));
    }

    @Override
    public ResponseEntity<PecaInsumoResponse> buscarPorId(@PathVariable Long id) throws ApplicationException {
        return ResponseEntity.ok(useCase.buscarPorId(id));
    }

    @Override
    public ResponseEntity<PecaInsumoResponse> atualizar(@PathVariable Long id, @RequestBody PecaInsumoRequest request) throws ApplicationException {
        return ResponseEntity.ok(useCase.atualizar(id, request));
    }

    @Override
    public ResponseEntity<Void> excluir(@PathVariable Long id) throws ApplicationException {
        useCase.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
