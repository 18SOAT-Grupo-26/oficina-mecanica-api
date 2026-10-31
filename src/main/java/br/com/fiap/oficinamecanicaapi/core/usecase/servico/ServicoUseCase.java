package br.com.fiap.oficinamecanicaapi.core.usecase.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.servico.ServicoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.servico.mapper.ServicoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ConflitoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ServicoUseCase implements ServicoUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(ServicoUseCase.class);


    private final ServicoPortOut servicoPortOut;
    private final ServicoMapper servicoMapper;
    private final MeterRegistry meterRegistry;

    public ServicoUseCase(ServicoPortOut servicoPortOut,
                          ServicoMapper servicoMapper,
                          MeterRegistry meterRegistry) {
        this.servicoPortOut = servicoPortOut;
        this.servicoMapper = servicoMapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        Servico servico = Servico.novo(request.nome(), request.descricao(), request.preco());
        validarNomeDisponivel(servico.getNomeNormalizado());

        Servico salvo = servicoPortOut.salvar(servico);

        contar("criado");
        log.info("action=servico_criado id={}", salvo.getId());
        return servicoMapper.toResponse(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<ServicoResponse> listar(Integer pagina, Integer tamanho) {
        return servicoPortOut.listarAtivos(Pagina.paginaValida(pagina), Pagina.tamanhoValido(tamanho)).map(servicoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(Long id) {
        return servicoMapper.toResponse(buscarServico(id));
    }

    @Override
    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico servico = buscarServico(id);
        String nomeAnterior = servico.getNomeNormalizado();
        BigDecimal precoAnterior = servico.getPreco();
        servico.atualizar(request.nome(), request.descricao(), request.preco());

        if (!servico.getNomeNormalizado().equals(nomeAnterior)) {
            validarNomeDisponivel(servico.getNomeNormalizado());
        }

        if (precoAnterior.compareTo(servico.getPreco()) != 0) {
            log.info("action=servico_preco_alterado id={} precoAnterior={} precoNovo={}",
                    id, precoAnterior, servico.getPreco());
        }
        Servico salvo = servicoPortOut.salvar(servico);

        contar("atualizado");
        log.info("action=servico_atualizado id={}", salvo.getId());
        return servicoMapper.toResponse(salvo);
    }

    @Override
    @Transactional
    public void desativar(Long id) {
        Servico servico = buscarServico(id);
        servico.desativar();
        servicoPortOut.salvar(servico);

        contar("desativado");
        log.info("action=servico_desativado id={}", id);
    }

    private Servico buscarServico(Long id) {
        return servicoPortOut.buscarAtivoPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
    }

    private void validarNomeDisponivel(String nomeNormalizado) {
        if (servicoPortOut.existePorNomeNormalizado(nomeNormalizado)) {
            throw new ConflitoException("Já existe um serviço cadastrado com este nome");
        }
    }

    private void contar(String operacao) {
        meterRegistry.counter("oficina.cadastro.operacoes", "entidade", "servico", "operacao", operacao).increment();
    }
}
