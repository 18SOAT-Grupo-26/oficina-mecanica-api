package br.com.fiap.oficinamecanicaapi.core.port.in.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface PecaInsumoUseCasePortIn {

    PecaInsumoResponse criarInsumo(PecaInsumoRequest request) throws ApplicationException;

}
