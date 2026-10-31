package br.com.fiap.oficinamecanicaapi.core.port.in.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

public interface ClienteUseCasePortIn {

    ClienteResponse criar(ClienteRequest request);

    ClienteResponse buscarPorId(Long id);

    /**
     * {@code documento} opcional (CPF ou CNPJ, com ou sem máscara): quando informado, a página traz no máximo esse cliente.
     */
    Pagina<ClienteResponse> listar(String documento, Integer pagina, Integer tamanho);

    ClienteResponse atualizar(Long id, ClienteRequest request);

    void desativar(Long id);
}
