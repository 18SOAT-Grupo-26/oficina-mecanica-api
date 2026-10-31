package br.com.fiap.oficinamecanicaapi.core.port.in.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

public interface ClienteUseCasePortIn {

    ClienteResponse criar(ClienteRequest request);

    ClienteResponse buscarPorId(Long id);

    ClienteResponse buscarPorDocumento(String documento);

    Pagina<ClienteResponse> listar(Integer pagina, Integer tamanho);

    ClienteResponse atualizar(Long id, ClienteRequest request);

    void desativar(Long id);
}
