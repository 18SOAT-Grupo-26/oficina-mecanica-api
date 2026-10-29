package br.com.fiap.oficinamecanicaapi.core.usecase.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecaInsumo.PecaInsumoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.pecaInsumo.PecaInsumoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.pecaInsumo.mapper.PecaInsumoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.NormalizadorNome;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class PecaInsumoUseCase implements PecaInsumoUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(PecaInsumoUseCase.class);

    static final int PAGINA_PADRAO = 0;
    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;

    private static final int NOME_MIN_LENGTH = 3;
    private static final int DESCRICAO_MAX_LENGTH = 500;
    private static final int PRECO_MAX_CASAS_DECIMAIS = 2;

    private final PecaInsumoPortOut pecaInsumoPortOut;
    private final PecaInsumoMapper pecaInsumoMapper;
    private final MeterRegistry meterRegistry;

    public PecaInsumoUseCase(PecaInsumoPortOut pecaInsumoPortOut,
                             PecaInsumoMapper pecaInsumoMapper,
                             MeterRegistry meterRegistry) {
        this.pecaInsumoPortOut = pecaInsumoPortOut;
        this.pecaInsumoMapper = pecaInsumoMapper;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public PecaInsumoResponse criar(PecaInsumoRequest request) throws ApplicationException {
        validarCampos(request);
        String nomeNormalizado = NormalizadorNome.normalizar(request.nome());
        validarNomeDisponivel(nomeNormalizado);

        PecaInsumo insumo = new PecaInsumo();
        preencher(insumo, request, nomeNormalizado);

        contar("criado");
        PecaInsumo salvo = pecaInsumoPortOut.salvar(insumo);

        log.info("action=insumo_criado id={}", salvo.getId());
        return pecaInsumoMapper.toResponse(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<PecaInsumoResponse> listar(Integer page, Integer size) {
        int paginaValida = page == null || page < 0 ? PAGINA_PADRAO : page;
        int tamanhoValido = size == null || size < 1 ? TAMANHO_PADRAO : Math.min(size, TAMANHO_MAXIMO);
        return pecaInsumoPortOut.listar(paginaValida, tamanhoValido).map(pecaInsumoMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PecaInsumoResponse buscarPorId(Long id) throws ApplicationException {
        return pecaInsumoMapper.toResponse(buscarInsumo(id));
    }

    @Override
    @Transactional
    public PecaInsumoResponse atualizar(Long id, PecaInsumoRequest request) throws ApplicationException {
        PecaInsumo insumo = buscarInsumo(id);
        validarCampos(request);

        String nomeNormalizado = NormalizadorNome.normalizar(request.nome());
        if (!nomeNormalizado.equals(insumo.getNomeNormalizado())) {
            validarNomeDisponivel(nomeNormalizado);
        }

        if (!Objects.equals(insumo.getQuantidade(), request.quantidade())) {
            log.info("action=insumo_estoque_ajustado id={} quantidadeAnterior={} quantidadeNova={}",
                    id, insumo.getQuantidade(), request.quantidade());
        }
        if (insumo.getPreco().compareTo(request.preco()) != 0) {
            log.info("action=insumo_preco_alterado id={} precoAnterior={} precoNovo={}",
                    id, insumo.getPreco(), request.preco());
        }
        preencher(insumo, request, nomeNormalizado);
        PecaInsumo salvo = pecaInsumoPortOut.salvar(insumo);

        contar("atualizado");

        log.info("action=insumo_atualizado id={}", salvo.getId());
        return pecaInsumoMapper.toResponse(salvo);
    }

    @Override
    @Transactional
    public void excluir(Long id) throws ApplicationException {
        PecaInsumo insumo = buscarInsumo(id);
        pecaInsumoPortOut.excluir(insumo);

        contar("excluido");
        log.info("action=insumo_excluido id={}", id);
    }

    private PecaInsumo buscarInsumo(Long id) {
        return pecaInsumoPortOut.buscarPorId(id)
                .orElseThrow(() -> new ApplicationException(404, "Insumo não encontrado"));
    }

    private void validarNomeDisponivel(String nomeNormalizado) {
        if (pecaInsumoPortOut.existePorNomeNormalizado(nomeNormalizado)) {
            throw new ApplicationException(409, "Já existe um insumo cadastrado com este nome");
        }
    }

    private static void validarCampos(PecaInsumoRequest request) {
        String nome = request.nome() == null ? null : request.nome().trim();
        if (nome == null || nome.isEmpty()) {
            throw new ApplicationException(422, "O campo nome é obrigatório");
        }
        if (nome.length() < NOME_MIN_LENGTH) {
            throw new ApplicationException(422, "O nome deve ter no mínimo 3 caracteres");
        }

        if (request.quantidade() == null) {
            throw new ApplicationException(422, "O campo quantidade é obrigatório");
        }
        if (request.quantidade() < 0) {
            throw new ApplicationException(422, "Quantidade inválida");
        }

        if (request.preco() == null) {
            throw new ApplicationException(422, "O campo preco é obrigatório");
        }
        if (request.preco().signum() <= 0
                || request.preco().stripTrailingZeros().scale() > PRECO_MAX_CASAS_DECIMAIS) {
            throw new ApplicationException(422, "Preço inválido");
        }

        int reservado = request.reservado() == null ? 0 : request.reservado();
        if (reservado < 0 || reservado > request.quantidade()) {
            throw new ApplicationException(422,
                    "Quantidade reservada inválida. Deve ser entre 0 e a quantidade em estoque");
        }

        if (request.descricao() != null && request.descricao().length() > DESCRICAO_MAX_LENGTH) {
            throw new ApplicationException(422, "A descrição deve ter no máximo 500 caracteres");
        }
    }

    private static void preencher(PecaInsumo insumo, PecaInsumoRequest request, String nomeNormalizado) {
        insumo.setNome(request.nome().trim());
        insumo.setNomeNormalizado(nomeNormalizado);
        insumo.setDescricao(request.descricao());
        insumo.setQuantidade(request.quantidade());
        insumo.setReservado(request.reservado() == null ? 0 : request.reservado());
        insumo.setPreco(request.preco().setScale(PRECO_MAX_CASAS_DECIMAIS));
    }

    private void contar(String operacao) {
        meterRegistry.counter("oficina.cadastro.operacoes", "entidade", "peca_insumo", "operacao", operacao).increment();
    }
}
