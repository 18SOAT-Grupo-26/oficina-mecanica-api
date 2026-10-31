package br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

public interface PecaInsumoUseCasePortIn {

    PecaInsumoResponse criar(PecaInsumoRequest request);

    Pagina<PecaInsumoResponse> listar(Integer pagina, Integer tamanho);

    PecaInsumoResponse buscarPorId(Long id);

    PecaInsumoResponse atualizar(Long id, PecaInsumoRequest request);

    void excluir(Long id);
}
