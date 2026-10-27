package br.com.fiap.oficinamecanicaapi.core.port.out.veiculo;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Placa;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Veiculo;

import java.util.Optional;

public interface VeiculoPortOut {

    Veiculo salvar(Veiculo veiculo);

    Optional<Veiculo> buscarAtivoPorId(Long id);

    Optional<Veiculo> buscarAtivoPorPlaca(Placa placa);

    /**
     * Considera também veículos inativos, pois a placa é única na tabela.
     */
    boolean existePorPlaca(Placa placa);

    /**
     * {@code clienteId} nulo lista todos os veículos ativos.
     */
    Pagina<Veiculo> listarAtivos(Long clienteId, int pagina, int tamanho);
}
