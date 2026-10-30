package br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface PecaInsumoUseCasePortIn {

    PecaInsumoResponse criar(PecaInsumoRequest request) throws ApplicationException;

    Pagina<PecaInsumoResponse> listar(Integer pagina, Integer tamanho);

    PecaInsumoResponse buscarPorId(Long id) throws ApplicationException;

    PecaInsumoResponse atualizar(Long id, PecaInsumoRequest request) throws ApplicationException;

    void excluir(Long id) throws ApplicationException;
}
