package br.com.fiap.oficinamecanicaapi.core.port.in.veiculo;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

public interface VeiculoUseCasePortIn {

    VeiculoResponse criar(VeiculoRequest request);

    VeiculoResponse buscarPorId(Long id);

    VeiculoResponse buscarPorPlaca(String placa);

    /**
     * {@code clienteId} opcional: quando informado, lista só os veículos desse cliente.
     */
    Pagina<VeiculoResponse> listar(Long clienteId, Integer pagina, Integer tamanho);

    VeiculoResponse atualizar(Long id, VeiculoRequest request);

    void desativar(Long id);
}
