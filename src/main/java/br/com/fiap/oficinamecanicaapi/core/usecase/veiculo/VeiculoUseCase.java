package br.com.fiap.oficinamecanicaapi.core.usecase.veiculo;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Placa;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Veiculo;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.VeiculoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.VeiculoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.veiculo.VeiculoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.veiculo.mapper.VeiculoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class VeiculoUseCase implements VeiculoUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(VeiculoUseCase.class);

    private final VeiculoPortOut veiculoPortOut;
    private final ClientePortOut clientePortOut;
    private final VeiculoMapper veiculoMapper;

    public VeiculoUseCase(VeiculoPortOut veiculoPortOut, ClientePortOut clientePortOut, VeiculoMapper veiculoMapper) {
        this.veiculoPortOut = veiculoPortOut;
        this.clientePortOut = clientePortOut;
        this.veiculoMapper = veiculoMapper;
    }

    @Override
    @Transactional
    public VeiculoResponse criar(VeiculoRequest request) throws ApplicationException {
        Placa placa = new Placa(request.placa());
        validarClienteAtivo(request.clienteId());
        validarPlacaDisponivel(placa);

        Veiculo veiculo = Veiculo.novo(placa, request.marca(), request.modelo(), request.ano(), request.clienteId());
        Veiculo salvo = veiculoPortOut.salvar(veiculo);

        log.info("action=veiculo_criado id={} clienteId={}", salvo.getId(), salvo.getClienteId());
        return veiculoMapper.toResponse(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorId(Long id) throws ApplicationException {
        return veiculoMapper.toResponse(buscarVeiculoAtivo(id));
    }

    @Override
    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorPlaca(String placa) throws ApplicationException {
        return veiculoPortOut.buscarAtivoPorPlaca(new Placa(placa))
                .map(veiculoMapper::toResponse)
                .orElseThrow(VeiculoUseCase::veiculoNaoEncontrado);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<VeiculoResponse> listar(Long clienteId, Integer pagina, Integer tamanho) {
        return veiculoPortOut.listarAtivos(clienteId, Pagina.paginaValida(pagina), Pagina.tamanhoValido(tamanho))
                .map(veiculoMapper::toResponse);
    }

    @Override
    @Transactional
    public VeiculoResponse atualizar(Long id, VeiculoRequest request) throws ApplicationException {
        Veiculo veiculo = buscarVeiculoAtivo(id);

        Placa placa = new Placa(request.placa());
        if (!Objects.equals(request.clienteId(), veiculo.getClienteId())) {
            validarClienteAtivo(request.clienteId());
        }
        if (!placa.equals(veiculo.getPlaca())) {
            validarPlacaDisponivel(placa);
        }

        veiculo.atualizar(placa, request.marca(), request.modelo(), request.ano(), request.clienteId());
        Veiculo salvo = veiculoPortOut.salvar(veiculo);

        log.info("action=veiculo_atualizado id={} clienteId={}", salvo.getId(), salvo.getClienteId());
        return veiculoMapper.toResponse(salvo);
    }

    @Override
    @Transactional
    public void desativar(Long id) throws ApplicationException {
        Veiculo veiculo = buscarVeiculoAtivo(id);
        veiculo.desativar();
        veiculoPortOut.salvar(veiculo);

        log.info("action=veiculo_desativado id={}", id);
    }

    private Veiculo buscarVeiculoAtivo(Long id) {
        return veiculoPortOut.buscarAtivoPorId(id).orElseThrow(VeiculoUseCase::veiculoNaoEncontrado);
    }

    private void validarClienteAtivo(Long clienteId) {
        if (clienteId == null || clientePortOut.buscarAtivoPorId(clienteId).isEmpty()) {
            throw new ApplicationException(404, "Cliente não encontrado");
        }
    }

    private void validarPlacaDisponivel(Placa placa) {
        if (veiculoPortOut.existePorPlaca(placa)) {
            throw new ApplicationException(409, "Já existe um veículo cadastrado com esta placa");
        }
    }

    private static ApplicationException veiculoNaoEncontrado() {
        return new ApplicationException(404, "Veículo não encontrado");
    }
}
