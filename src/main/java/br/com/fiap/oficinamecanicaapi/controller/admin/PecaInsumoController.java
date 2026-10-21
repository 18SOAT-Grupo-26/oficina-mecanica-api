package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecaInsumo.PecaInsumoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/insumos")
public class PecaInsumoController implements PecaInsumoAPI {
    private static final Logger log = LoggerFactory.getLogger(PecaInsumoController.class);

    private final PecaInsumoUseCasePortIn useCase;

    public PecaInsumoController(PecaInsumoUseCasePortIn useCase) {
        this.useCase = useCase;
    }

    @Override
    public ResponseEntity<PecaInsumoResponse> criarInsumo(@RequestBody PecaInsumoRequest request) throws ApplicationException {
        log.info("Creating new insumo");
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.criarInsumo(request));
    }
}
