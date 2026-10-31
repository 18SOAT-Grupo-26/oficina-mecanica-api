package br.com.fiap.oficinamecanicaapi.core.port.in.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface ClienteUseCasePortIn {

    ClienteResponse criar(ClienteRequest request) throws ApplicationException;

    ClienteResponse buscarPorId(Long id) throws ApplicationException;

    ClienteResponse buscarPorDocumento(String documento) throws ApplicationException;

    Pagina<ClienteResponse> listar(Integer pagina, Integer tamanho);

    ClienteResponse atualizar(Long id, ClienteRequest request) throws ApplicationException;

    void desativar(Long id) throws ApplicationException;
}
