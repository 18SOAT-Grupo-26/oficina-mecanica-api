package br.com.fiap.oficinamecanicaapi.core.port.in.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface ServicoUseCasePortIn {

    ServicoResponse criar(ServicoRequest request) throws ApplicationException;

    Pagina<ServicoResponse> listar(Integer page, Integer size);

    ServicoResponse buscarPorId(Long id) throws ApplicationException;

    ServicoResponse atualizar(Long id, ServicoRequest request) throws ApplicationException;

    void excluir(Long id) throws ApplicationException;
}
