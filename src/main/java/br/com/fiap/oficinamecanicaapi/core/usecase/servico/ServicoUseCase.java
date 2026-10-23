package br.com.fiap.oficinamecanicaapi.core.usecase.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.entity.PecaInsumoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.entity.ServicoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.pecaInsumo.PecaInsumoPortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.servico.ServicoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.servico.mapper.ServicoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.NormalizadorNome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServicoUseCase implements ServicoUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(ServicoUseCase.class);

    static final int PAGINA_PADRAO = 0;
    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;

    private static final int NOME_MIN_LENGTH = 3;
    private static final int DESCRICAO_MAX_LENGTH = 500;
    private static final int PRECO_MAX_CASAS_DECIMAIS = 2;

    private final ServicoPortOut servicoPortOut;
    private final PecaInsumoPortOut pecaInsumoPortOut;
    private final ServicoMapper servicoMapper;

    public ServicoUseCase(ServicoPortOut servicoPortOut,
                          PecaInsumoPortOut pecaInsumoPortOut,
                          ServicoMapper servicoMapper) {
        this.servicoPortOut = servicoPortOut;
        this.pecaInsumoPortOut = pecaInsumoPortOut;
        this.servicoMapper = servicoMapper;
    }

    @Override
    public ServicoResponse criar(ServicoRequest request) throws ApplicationException {
        validarCampos(request);
        String nomeNormalizado = NormalizadorNome.normalizar(request.nome());
        validarNomeDisponivel(nomeNormalizado);

        ServicoEntity entity = new ServicoEntity();
        preencher(entity, request, nomeNormalizado);
        ServicoEntity salvo = servicoPortOut.salvar(entity);

        log.info("action=servico_criado id={}", salvo.getId());
        return servicoMapper.toResponse(salvo);
    }

    @Override
    public Pagina<ServicoResponse> listar(Integer page, Integer size) {
        int paginaValida = page == null || page < 0 ? PAGINA_PADRAO : page;
        int tamanhoValido = size == null || size < 1 ? TAMANHO_PADRAO : Math.min(size, TAMANHO_MAXIMO);
        return servicoPortOut.listar(paginaValida, tamanhoValido).map(servicoMapper::toResponse);
    }

    @Override
    public ServicoResponse buscarPorId(Long id) throws ApplicationException {
        return servicoMapper.toResponse(buscarServico(id));
    }

    @Override
    public ServicoResponse atualizar(Long id, ServicoRequest request) throws ApplicationException {
        ServicoEntity entity = buscarServico(id);
        validarCampos(request);

        String nomeNormalizado = NormalizadorNome.normalizar(request.nome());
        if (!nomeNormalizado.equals(entity.getNomeNormalizado())) {
            validarNomeDisponivel(nomeNormalizado);
        }

        preencher(entity, request, nomeNormalizado);
        ServicoEntity salvo = servicoPortOut.salvar(entity);

        log.info("action=servico_atualizado id={}", salvo.getId());
        return servicoMapper.toResponse(salvo);
    }

    @Override
    public void excluir(Long id) throws ApplicationException {
        ServicoEntity entity = buscarServico(id);
        servicoPortOut.excluir(entity);

        log.info("action=servico_excluido id={}", id);
    }

    private ServicoEntity buscarServico(Long id) {
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

    private void preencher(ServicoEntity entity, ServicoRequest request, String nomeNormalizado) {
        entity.setNome(request.nome().trim());
        entity.setNomeNormalizado(nomeNormalizado);
        entity.setDescricao(request.descricao());
        entity.setPreco(request.preco().setScale(PRECO_MAX_CASAS_DECIMAIS));
        entity.setPecasInsumo(resolverPecasInsumo(request.pecasInsumoIds()));
    }

    private List<PecaInsumoEntity> resolverPecasInsumo(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> idsUnicos = ids.stream().distinct().toList();
        List<PecaInsumoEntity> pecas = pecaInsumoPortOut.buscarPorIds(idsUnicos);
        if (pecas.size() != idsUnicos.size()) {
            throw new ApplicationException(422, "Um ou mais insumos informados não existem");
        }
        return new ArrayList<>(pecas);
    }
}
