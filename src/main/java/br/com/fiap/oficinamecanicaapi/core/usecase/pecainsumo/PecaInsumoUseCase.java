package br.com.fiap.oficinamecanicaapi.core.usecase.pecainsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecainsumo.PecaInsumoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.pecainsumo.PecaInsumoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.pecainsumo.mapper.PecaInsumoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ConflitoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public class PecaInsumoUseCase implements PecaInsumoUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(PecaInsumoUseCase.class);

    static final int PAGINA_PADRAO = 0;
    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;


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
    public PecaInsumoResponse criar(PecaInsumoRequest request) {
        PecaInsumo insumo = PecaInsumo.novo(request.nome(), request.descricao(), request.quantidade(),
                request.reservado(), request.preco());
        validarNomeDisponivel(insumo.getNomeNormalizado());

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
    public PecaInsumoResponse buscarPorId(Long id) {
        return pecaInsumoMapper.toResponse(buscarInsumo(id));
    }

    @Override
    @Transactional
    public PecaInsumoResponse atualizar(Long id, PecaInsumoRequest request) {
        PecaInsumo insumo = buscarInsumo(id);
        String nomeAnterior = insumo.getNomeNormalizado();
        Integer quantidadeAnterior = insumo.getQuantidade();
        BigDecimal precoAnterior = insumo.getPreco();
        insumo.atualizar(request.nome(), request.descricao(), request.quantidade(), request.reservado(),
                request.preco());

        if (!insumo.getNomeNormalizado().equals(nomeAnterior)) {
            validarNomeDisponivel(insumo.getNomeNormalizado());
        }

        if (!Objects.equals(quantidadeAnterior, insumo.getQuantidade())) {
            log.info("action=insumo_estoque_ajustado id={} quantidadeAnterior={} quantidadeNova={}",
                    id, quantidadeAnterior, insumo.getQuantidade());
        }

        if (precoAnterior.compareTo(insumo.getPreco()) != 0) {
            log.info("action=insumo_preco_alterado id={} precoAnterior={} precoNovo={}",
                    id, precoAnterior, insumo.getPreco());
        }

        PecaInsumo salvo = pecaInsumoPortOut.salvar(insumo);
        contar("atualizado");
        log.info("action=insumo_atualizado id={}", salvo.getId());
        return pecaInsumoMapper.toResponse(salvo);
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        PecaInsumo insumo = buscarInsumo(id);
        pecaInsumoPortOut.excluir(insumo);

        contar("excluido");
        log.info("action=insumo_excluido id={}", id);
    }

    private PecaInsumo buscarInsumo(Long id) {
        return pecaInsumoPortOut.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Insumo não encontrado"));
    }

    private void validarNomeDisponivel(String nomeNormalizado) {
        if (pecaInsumoPortOut.existePorNomeNormalizado(nomeNormalizado)) {
            throw new ConflitoException("Já existe um insumo cadastrado com este nome");
        }
    }

    private void contar(String operacao) {
        meterRegistry.counter("oficina.cadastro.operacoes", "entidade", "peca_insumo", "operacao", operacao).increment();
    }
}
