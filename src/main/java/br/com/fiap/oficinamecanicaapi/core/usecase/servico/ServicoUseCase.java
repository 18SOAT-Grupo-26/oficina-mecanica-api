package br.com.fiap.oficinamecanicaapi.core.usecase.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.servico.ServicoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.servico.mapper.ServicoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.NormalizadorNome;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoUseCase implements ServicoUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(ServicoUseCase.class);

    private static final int NOME_MIN_LENGTH = 3;
    private static final int DESCRICAO_MAX_LENGTH = 500;
    private static final int PRECO_MAX_CASAS_DECIMAIS = 2;

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
    public ServicoResponse criar(ServicoRequest request) throws ApplicationException {
        validarCampos(request);
        String nomeNormalizado = NormalizadorNome.normalizar(request.nome());
        validarNomeDisponivel(nomeNormalizado);

        Servico servico = new Servico();
        preencher(servico, request, nomeNormalizado);
        Servico salvo = servicoPortOut.salvar(servico);

        contar("criado");
        log.info("action=servico_criado id={}", salvo.getId());
        return servicoMapper.toResponse(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<ServicoResponse> listar(Integer page, Integer size) {
        return servicoPortOut.listar(Pagina.paginaValida(page), Pagina.tamanhoValido(size)).map(servicoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicoResponse buscarPorId(Long id) throws ApplicationException {
        return servicoMapper.toResponse(buscarServico(id));
    }

    @Override
    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest request) throws ApplicationException {
        Servico servico = buscarServico(id);
        validarCampos(request);

        String nomeNormalizado = NormalizadorNome.normalizar(request.nome());
        if (!nomeNormalizado.equals(servico.getNomeNormalizado())) {
            validarNomeDisponivel(nomeNormalizado);
        }

        if (servico.getPreco().compareTo(request.preco()) != 0) {
            log.info("action=servico_preco_alterado id={} precoAnterior={} precoNovo={}",
                    id, servico.getPreco(), request.preco());
        }
        preencher(servico, request, nomeNormalizado);
        Servico salvo = servicoPortOut.salvar(servico);

        contar("atualizado");
        log.info("action=servico_atualizado id={}", salvo.getId());
        return servicoMapper.toResponse(salvo);
    }

    @Override
    @Transactional
    public void excluir(Long id) throws ApplicationException {
        Servico servico = buscarServico(id);
        servicoPortOut.excluir(servico);

        contar("excluido");
        log.info("action=servico_excluido id={}", id);
    }

    private Servico buscarServico(Long id) {
        return servicoPortOut.buscarPorId(id)
                .orElseThrow(() -> new ApplicationException(404, "Serviço não encontrado"));
    }

    private void validarNomeDisponivel(String nomeNormalizado) {
        if (servicoPortOut.existePorNomeNormalizado(nomeNormalizado)) {
            throw new ApplicationException(409, "Já existe um serviço cadastrado com este nome");
        }
    }

    private static void validarCampos(ServicoRequest request) {
        String nome = request.nome() == null ? null : request.nome().trim();
        if (nome == null || nome.isEmpty()) {
            throw new ApplicationException(422, "O campo nome é obrigatório");
        }
        if (nome.length() < NOME_MIN_LENGTH) {
            throw new ApplicationException(422, "O nome deve ter no mínimo 3 caracteres");
        }

        if (request.preco() == null) {
            throw new ApplicationException(422, "O campo preco é obrigatório");
        }
        if (request.preco().signum() <= 0
                || request.preco().stripTrailingZeros().scale() > PRECO_MAX_CASAS_DECIMAIS) {
            throw new ApplicationException(422, "Preço inválido");
        }

        if (request.descricao() != null && request.descricao().length() > DESCRICAO_MAX_LENGTH) {
            throw new ApplicationException(422, "A descrição deve ter no máximo 500 caracteres");
        }
    }

    private void preencher(Servico servico, ServicoRequest request, String nomeNormalizado) {
        servico.setNome(request.nome().trim());
        servico.setNomeNormalizado(nomeNormalizado);
        servico.setDescricao(request.descricao());
        servico.setPreco(request.preco().setScale(PRECO_MAX_CASAS_DECIMAIS));
    }

    private void contar(String operacao) {
        meterRegistry.counter("oficina.cadastro.operacoes", "entidade", "servico", "operacao", operacao).increment();
    }
}
