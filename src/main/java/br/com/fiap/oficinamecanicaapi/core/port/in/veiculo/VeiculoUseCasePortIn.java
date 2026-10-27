package br.com.fiap.oficinamecanicaapi.core.port.in.veiculo;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.VeiculoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.VeiculoResponse;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface VeiculoUseCasePortIn {

    VeiculoResponse criar(VeiculoRequest request) throws ApplicationException;

    VeiculoResponse buscarPorId(Long id) throws ApplicationException;

    VeiculoResponse buscarPorPlaca(String placa) throws ApplicationException;

    /**
     * {@code clienteId} opcional: quando informado, lista só os veículos desse cliente.
     */
    Pagina<VeiculoResponse> listar(Long clienteId, Integer pagina, Integer tamanho);

    VeiculoResponse atualizar(Long id, VeiculoRequest request) throws ApplicationException;

    void desativar(Long id) throws ApplicationException;
}
