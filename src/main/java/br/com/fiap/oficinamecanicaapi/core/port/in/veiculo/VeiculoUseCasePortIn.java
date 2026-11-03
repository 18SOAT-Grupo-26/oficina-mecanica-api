package br.com.fiap.oficinamecanicaapi.core.port.in.veiculo;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;

public interface VeiculoUseCasePortIn {

    VeiculoResponse criar(VeiculoRequest request);

    VeiculoResponse buscarPorId(Long id);

    /**
     * Filtros opcionais: {@code clienteId} lista só os veículos desse cliente; {@code placa} (com ou sem hífen)
     * traz no máximo esse veículo.
     */
    Pagina<VeiculoResponse> listar(Long clienteId, String placa, Integer pagina, Integer tamanho);

    VeiculoResponse atualizar(Long id, VeiculoRequest request);

    void desativar(Long id);
}
