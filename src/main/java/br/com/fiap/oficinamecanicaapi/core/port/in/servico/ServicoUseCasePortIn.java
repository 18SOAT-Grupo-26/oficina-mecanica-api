package br.com.fiap.oficinamecanicaapi.core.port.in.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

public interface ServicoUseCasePortIn {

    ServicoResponse criar(ServicoRequest request);

    Pagina<ServicoResponse> listar(Integer pagina, Integer tamanho);

    ServicoResponse buscarPorId(Long id);

    ServicoResponse atualizar(Long id, ServicoRequest request);

    void desativar(Long id);
}
