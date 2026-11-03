package br.com.fiap.oficinamecanicaapi.core.usecase.veiculo;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Placa;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Veiculo;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.veiculo.VeiculoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.veiculo.mapper.VeiculoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ConflitoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final MeterRegistry meterRegistry;

    public VeiculoUseCase(VeiculoPortOut veiculoPortOut,
                          ClientePortOut clientePortOut,
                          VeiculoMapper veiculoMapper,
                          MeterRegistry meterRegistry) {
        this.veiculoPortOut = veiculoPortOut;
        this.clientePortOut = clientePortOut;
        this.veiculoMapper = veiculoMapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public VeiculoResponse criar(VeiculoRequest request) {
        Placa placa = new Placa(request.placa());
        validarClienteAtivo(request.clienteId());
        validarPlacaDisponivel(placa);

        Veiculo veiculo = Veiculo.novo(placa, request.marca(), request.modelo(), request.ano(), request.clienteId());
        Veiculo salvo = veiculoPortOut.salvar(veiculo);

        contar("criado");
        log.info("action=veiculo_criado id={} clienteId={}", salvo.getId(), salvo.getClienteId());
        return veiculoMapper.toResponse(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorId(Long id) {
        return veiculoMapper.toResponse(buscarVeiculoAtivo(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<VeiculoResponse> listar(Long clienteId, String placa, Integer pagina, Integer tamanho) {
        int numero = Pagina.paginaValida(pagina);
        int itensPorPagina = Pagina.tamanhoValido(tamanho);
        Pagina<Veiculo> veiculos = placa == null
                ? veiculoPortOut.listarAtivos(clienteId, numero, itensPorPagina)
                : Pagina.deNoMaximoUm(veiculoPortOut.buscarAtivoPorPlaca(new Placa(placa))
                        .filter(veiculo -> clienteId == null || clienteId.equals(veiculo.getClienteId())), numero, itensPorPagina);
        return veiculos.map(veiculoMapper::toResponse);
    }

    @Override
    @Transactional
    public VeiculoResponse atualizar(Long id, VeiculoRequest request) {
        Veiculo veiculo = buscarVeiculoAtivo(id);

        Placa placa = new Placa(request.placa());
        if (!Objects.equals(request.clienteId(), veiculo.getClienteId())) {
            validarClienteAtivo(request.clienteId());
            log.info("action=veiculo_transferido id={} clienteAnterior={} clienteNovo={}", id, veiculo.getClienteId(), request.clienteId());
        }
        if (!placa.equals(veiculo.getPlaca())) {
            validarPlacaDisponivel(placa);
        }

        veiculo.atualizar(placa, request.marca(), request.modelo(), request.ano(), request.clienteId());
        Veiculo salvo = veiculoPortOut.salvar(veiculo);

        contar("atualizado");
        log.info("action=veiculo_atualizado id={} clienteId={}", salvo.getId(), salvo.getClienteId());
        return veiculoMapper.toResponse(salvo);
    }

    @Override
    @Transactional
    public void desativar(Long id) {
        Veiculo veiculo = buscarVeiculoAtivo(id);
        veiculo.desativar();
        veiculoPortOut.salvar(veiculo);

        contar("desativado");
        log.info("action=veiculo_desativado id={}", id);
    }

    private Veiculo buscarVeiculoAtivo(Long id) {
        return veiculoPortOut.buscarAtivoPorId(id).orElseThrow(VeiculoUseCase::veiculoNaoEncontrado);
    }

    private void validarClienteAtivo(Long clienteId) {
        if (clienteId == null || clientePortOut.buscarAtivoPorId(clienteId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado");
        }
    }

    private void validarPlacaDisponivel(Placa placa) {
        if (veiculoPortOut.existePorPlaca(placa)) {
            throw new ConflitoException("Já existe um veículo cadastrado com esta placa");
        }
    }

    private static RecursoNaoEncontradoException veiculoNaoEncontrado() {
        return new RecursoNaoEncontradoException("Veículo não encontrado");
    }

    private void contar(String operacao) {
        meterRegistry.counter("oficina.cadastro.operacoes", "entidade", "veiculo", "operacao", operacao).increment();
    }
}
