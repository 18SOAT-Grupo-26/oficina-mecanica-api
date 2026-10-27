package br.com.fiap.oficinamecanicaapi.core.usecase.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.*;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.service.CalculadoraTempoMedioExecucao;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.entity.PecaInsumoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.entity.ServicoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Veiculo;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.ordemservico.OrdemServicoPortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.pecaInsumo.PecaInsumoPortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.servico.ServicoPortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.veiculo.VeiculoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.ordemservico.mapper.OrdemServicoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * Casos de uso da OS. As regras de status e de itens ficam no agregado; aqui ficam as consultas aos
 * cadastros (cliente, veículo, serviço e peça, pelos ports de cada dono), a conferência de
 * saldo na inclusão de peça, a tradução do PUT e do pedido de status em comandos do agregado e a métrica do tempo de execução.
 * Cada operação roda em uma transação: se algo falhar no meio, nada é gravado.
 */
@Service
@Transactional
public class OrdemServicoUseCase implements OrdemServicoUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(OrdemServicoUseCase.class);

    private final OrdemServicoPortOut ordemServicoPortOut;
    private final ClientePortOut clientePortOut;
    private final VeiculoPortOut veiculoPortOut;
    private final ServicoPortOut servicoPortOut;
    private final PecaInsumoPortOut pecaInsumoPortOut;
    private final OrdemServicoMapper mapper;
    private final Clock clock;
    private final Timer tempoExecucao;
    private final Timer tempoTotal;
    private final DistributionSummary valorOrcamento;
    private final MeterRegistry meterRegistry;

    public OrdemServicoUseCase(OrdemServicoPortOut ordemServicoPortOut, ClientePortOut clientePortOut,
                               VeiculoPortOut veiculoPortOut, ServicoPortOut servicoPortOut,
                               PecaInsumoPortOut pecaInsumoPortOut, OrdemServicoMapper mapper, Clock clock,
                               MeterRegistry meterRegistry) {
        this.ordemServicoPortOut = ordemServicoPortOut;
        this.clientePortOut = clientePortOut;
        this.veiculoPortOut = veiculoPortOut;
        this.servicoPortOut = servicoPortOut;
        this.pecaInsumoPortOut = pecaInsumoPortOut;
        this.mapper = mapper;
        this.clock = clock;
        // registrado já na subida, para aparecer no /actuator/prometheus (com contagem zero) antes da primeira OS finalizada
        this.tempoExecucao = Timer.builder("oficina.os.tempo.execucao")
                .description("Tempo de execução da OS, de EM_EXECUCAO até FINALIZADA")
                .register(meterRegistry);
        this.tempoTotal = Timer.builder("oficina.os.tempo.total")
                .description("Tempo total da OS, da abertura (RECEBIDA) até ENTREGUE")
                .register(meterRegistry);
        this.valorOrcamento = DistributionSummary.builder("oficina.os.orcamento.valor")
                .description("Valor do orçamento enviado ao cliente")
                .baseUnit("reais")
                .register(meterRegistry);
        this.meterRegistry = meterRegistry;
    }

    @Override
    public OrdemServicoResponse criar(OrdemServicoRequest request) {
        exigirCamposObrigatorios(request);
        Cliente cliente = buscarCliente(request.documentoCliente());
        Veiculo veiculo = buscarVeiculoDoCliente(request.idVeiculo(), cliente.getId());

        OrdemServico os = OrdemServico.abrir(cliente.getId(), veiculo.getId(), agora());
        sincronizarServicos(os, request.servicos());
        sincronizarPecas(os, request.pecas());
        OrdemServico salva = ordemServicoPortOut.salvar(os);

        meterRegistry.counter("oficina.os.criadas").increment();
        log.info("action=os_criada id={} clienteId={} veiculoId={} servicos={} pecas={}", salva.getId(),
                salva.getIdCliente(), salva.getIdVeiculo(), salva.getItensServico().size(), salva.getItensPeca().size());
        return mapper.toResponse(salva);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemServicoResponse consultar(Long id) throws ApplicationException {
        return mapper.toResponse(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemServicoResponse> listar(Long idCliente) throws ApplicationException {
        return mapper.toResponseList(listarOrdens(idCliente));
    }

    private List<OrdemServico> listarOrdens(Long idCliente) {
        return ordemServicoPortOut.listarOrdens(idCliente);
    }

    @Override
    public OrdemServicoResponse atualizar(Long id, OrdemServicoRequest request) {
        exigirCamposObrigatorios(request);
        OrdemServico os = buscar(id);
        os.exigirEditavel();

        exigirMesmoCliente(os, request.documentoCliente());
        exigirMesmoVeiculo(os, request.idVeiculo());
        sincronizarServicos(os, request.servicos());
        sincronizarPecas(os, request.pecas());
        OrdemServicoResponse response = mapper.toResponse(ordemServicoPortOut.salvar(os));

        log.info("action=os_atualizada id={} status={}", id, response.status());
        return response;
    }

    @Override
    public OrdemServicoResponse alterarStatus(Long id, OrdemServicoRequest.Status request) {
        if (request == null || request.status() == null) {
            throw new ApplicationException(400, "O status de destino é obrigatório");
        }
        OrdemServico os = buscar(id);
        StatusOrdemServico statusAnterior = os.getStatus();

        aplicarStatus(os, request.status());
        OrdemServicoResponse response = mapper.toResponse(ordemServicoPortOut.salvar(os));

        contarTransicao(statusAnterior, response.status());
        log.info("action=os_status_alterado id={} statusAnterior={} status={}", id, statusAnterior, response.status());
        return response;
    }

    @Override
    public OrdemServicoResponse registrarOrcamentoEnviado(Long id) {
        return registrarOrcamento(id, OrdemServico::aguardarAprovacao, AcaoEstoque.RESERVAR,
                "os_orcamento_enviado");
    }

    @Override
    public OrdemServicoResponse registrarOrcamentoAprovado(Long id) {
        return registrarOrcamento(id, OrdemServico::iniciarExecucao, AcaoEstoque.CONSUMIR,
                "os_orcamento_aprovado");
    }

    @Override
    public OrdemServicoResponse registrarOrcamentoRejeitado(Long id) {
        return registrarOrcamento(id, OrdemServico::rejeitarOrcamento, AcaoEstoque.LIBERAR,
                "os_orcamento_rejeitado");
    }

    @Override
    @Transactional(readOnly = true)
    public TempoMedioExecucaoResponse consultarTempoMedioExecucao() {
        List<Duration> tempos = ordemServicoPortOut.listarTemposExecucao();
        int quantidadeOrdens = tempos.size();
        Long mediaMinutos = CalculadoraTempoMedioExecucao.calcular(tempos)
                .map(Duration::toMinutes)
                .orElse(null);
        return new TempoMedioExecucaoResponse(mediaMinutos, quantidadeOrdens);
    }

    // ---- dados -----------------------------------------------------------------------------------

    /*
     * O PUT é completo: traz sempre cliente, veículo e as duas listas de itens. Cliente e veículo não
     * mudam depois da abertura, então só são conferidos; os itens são sincronizados, e item igual ao
     * atual não chama o agregado.
     */

    /**
     * A validação do controller cobre o mesmo, mas o caso de uso não depende dela.
     */
    private static void exigirCamposObrigatorios(OrdemServicoRequest request) {
        if (request == null) {
            throw new ApplicationException(400, "Os dados da OS são obrigatórios");
        }
        if (request.documentoCliente() == null || request.documentoCliente().isBlank()) {
            throw new ApplicationException(400, "O cliente da OS é obrigatório");
        }
        if (request.idVeiculo() == null) {
            throw new ApplicationException(400, "O veículo da OS é obrigatório");
        }
        if (request.servicos() == null) {
            throw new ApplicationException(400, "Os serviços da OS são obrigatórios (envie uma lista vazia se não houver)");
        }
        if (request.pecas() == null) {
            throw new ApplicationException(400, "As peças da OS são obrigatórias (envie uma lista vazia se não houver)");
        }
    }

    /**
     * O documento chega no PUT, mas a OS guarda o id: busca o cliente só para comparar.
     */
    private void exigirMesmoCliente(OrdemServico os, String documentoCliente) {
        if (!buscarCliente(documentoCliente).getId().equals(os.getIdCliente())) {
            throw new ApplicationException(409, "O cliente da OS não pode ser alterado; cancele a OS e abra outra");
        }
    }

    private static void exigirMesmoVeiculo(OrdemServico os, Long idVeiculo) {
        if (!idVeiculo.equals(os.getIdVeiculo())) {
            throw new ApplicationException(409, "O veículo da OS não pode ser alterado; cancele a OS e abra outra");
        }
    }

    private Cliente buscarCliente(String documentoCliente) {
        Documento documento = new Documento(documentoCliente);
        return clientePortOut.buscarAtivoPorDocumento(documento)
                .orElseThrow(() -> new ApplicationException(404, "Cliente não encontrado"));
    }

    private Veiculo buscarVeiculoDoCliente(Long idVeiculo, Long idCliente) {
        Veiculo veiculo = veiculoPortOut.buscarAtivoPorId(idVeiculo)
                .orElseThrow(() -> new ApplicationException(404, "Veículo não encontrado"));
        if (!veiculo.getClienteId().equals(idCliente)) {
            throw new ApplicationException(409, "O veículo não pertence ao cliente da OS");
        }
        return veiculo;
    }

    /**
     * Item novo vem do cadastro; item que só mudou de quantidade mantém o preço já congelado.
     */
    private void sincronizarServicos(OrdemServico os, List<OrdemServicoRequest.Servico> servicos) {
        Map<Long, Integer> desejado = porId(servicos, OrdemServicoRequest.Servico::idServico,
                OrdemServicoRequest.Servico::quantidade, "Serviço repetido na requisição: ");

        for (ItemServico atual : List.copyOf(os.getItensServico())) {
            Integer quantidade = desejado.remove(atual.getIdServico());
            if (quantidade == null) {
                os.removerServico(atual.getIdServico());
            } else if (quantidade != atual.getQuantidade()) {
                os.removerServico(atual.getIdServico());
                os.incluirServico(atual.getIdServico(), atual.getDescricao(), atual.getPrecoUnitario(), quantidade);
            }
        }
        desejado.forEach((idServico, quantidade) -> {
            ServicoEntity servico = servicoPortOut.buscarPorId(idServico)
                    .orElseThrow(() -> new ApplicationException(404, "Serviço não encontrado no catálogo"));
            os.incluirServico(servico.getId(), servico.getNome(), new ValorMonetario(servico.getPreco()), quantidade);
        });
    }

    /**
     * Como os serviços, mas conferindo o saldo; a reserva fica com o orçamento, no envio.
     */
    private void sincronizarPecas(OrdemServico os, List<OrdemServicoRequest.Peca> pecas) {
        Map<Long, Integer> desejado = porId(pecas, OrdemServicoRequest.Peca::idPeca,
                OrdemServicoRequest.Peca::quantidade, "Peça repetida na requisição: ");

        for (ItemPeca atual : List.copyOf(os.getItensPeca())) {
            Integer quantidade = desejado.remove(atual.getIdPeca());
            if (quantidade == null) {
                os.removerPeca(atual.getIdPeca());
            } else if (quantidade != atual.getQuantidade()) {
                os.removerPeca(atual.getIdPeca());
                exigirSaldo(buscarPeca(atual.getIdPeca()), quantidade);
                os.incluirPeca(atual.getIdPeca(), atual.getDescricao(), atual.getPrecoUnitario(), quantidade);
            }
        }
        desejado.forEach((idPeca, quantidade) -> {
            PecaInsumoEntity peca = buscarPeca(idPeca);
            exigirSaldo(peca, quantidade);
            os.incluirPeca(peca.getId(), peca.getNome(), new ValorMonetario(peca.getPreco()), quantidade);
        });
    }

    private PecaInsumoEntity buscarPeca(Long idPeca) {
        return pecaInsumoPortOut.buscarPorId(idPeca)
                .orElseThrow(() -> new ApplicationException(404, "Peça não encontrada no catálogo"));
    }

    /**
     * Saldo disponível = quantidade em estoque menos a já reservada para orçamentos enviados.
     */
    private static void exigirSaldo(PecaInsumoEntity peca, int quantidade) {
        int saldo = valorOuZero(peca.getQuantidade()) - valorOuZero(peca.getReservado());
        if (quantidade > saldo) {
            throw new ApplicationException(409, "Saldo insuficiente para a peça " + peca.getNome()
                    + ": disponível " + saldo + ", solicitado " + quantidade);
        }
    }

    private static int valorOuZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private static <T> Map<Long, Integer> porId(List<T> itens, Function<T, Long> id, ToIntFunction<T> quantidade,
                                                String mensagemRepetido) {
        Map<Long, Integer> porId = new LinkedHashMap<>();
        for (T item : itens) {
            if (porId.put(id.apply(item), quantidade.applyAsInt(item)) != null) {
                throw new ApplicationException(409, mensagemRepetido + id.apply(item));
            }
        }
        return porId;
    }

    // ---- status ----------------------------------------------------------------------------------

    /**
     * Mudanças manuais. Pedir o status atual também é transição inválida: o status só avança.
     * EM_DIAGNOSTICO vem da abertura; AGUARDANDO_APROVACAO e EM_EXECUCAO, do orçamento. CANCELADA é o
     * cancelamento da OS, que não a remove; com o orçamento pendente, libera as peças reservadas (ADR 003).
     */
    private void aplicarStatus(OrdemServico os, StatusOrdemServico destino) {
        switch (destino) {
            case FINALIZADA -> {
                os.finalizar(agora());
                os.getTempoExecucao().ifPresent(tempoExecucao::record);
            }
            case ENTREGUE -> {
                os.entregar(agora());
                os.getHistorico().stream().findFirst()
                        .map(abertura -> Duration.between(abertura.dataHora(), agora()))
                        .ifPresent(tempoTotal::record);
            }
            case CANCELADA -> {
                boolean temReserva = os.getStatus() == StatusOrdemServico.AGUARDANDO_APROVACAO;
                os.cancelar(agora());
                if (temReserva) {
                    atualizarEstoqueOrcamento(os, AcaoEstoque.LIBERAR);
                }
            }
            case EM_DIAGNOSTICO ->
                    throw new ApplicationException(409, "EM_DIAGNOSTICO é automático: a OS entra nele ao ser aberta");
            case AGUARDANDO_APROVACAO ->
                    throw new ApplicationException(409, "AGUARDANDO_APROVACAO é automático: vem do envio do orçamento");
            case EM_EXECUCAO ->
                    throw new ApplicationException(409, "EM_EXECUCAO é automático: vem da aprovação do orçamento");
            case RECEBIDA ->
                    throw new ApplicationException(409, "Transição inválida: " + os.getStatus() + " → RECEBIDA");
        }
    }

    private OrdemServicoResponse registrarOrcamento(Long id, BiConsumer<OrdemServico, OffsetDateTime> comando,
                                                    AcaoEstoque acaoEstoque, String acao) {
        OrdemServico os = buscar(id);
        StatusOrdemServico statusAnterior = os.getStatus();
        comando.accept(os, agora());
        atualizarEstoqueOrcamento(os, acaoEstoque);

        OrdemServicoResponse response = mapper.toResponse(ordemServicoPortOut.salvar(os));
        contarTransicao(statusAnterior, response.status());
        if (acaoEstoque == AcaoEstoque.RESERVAR) {
            valorOrcamento.record(valorTotal(os).doubleValue());
        }

        log.info("action={} id={} status={}", acao, id, response.status());
        return response;
    }

    private void atualizarEstoqueOrcamento(OrdemServico os, AcaoEstoque acao) {
        Map<Long, Integer> quantidades = new LinkedHashMap<>();
        Map<Long, String> descricoes = new LinkedHashMap<>();
        for (ItemPeca item : os.getItensPeca()) {
            quantidades.merge(item.getIdPeca(), item.getQuantidade(), Math::addExact);
            descricoes.putIfAbsent(item.getIdPeca(), item.getDescricao());
        }

        List<MovimentacaoEstoque> movimentacoes = quantidades.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MovimentacaoEstoque(entry.getKey(), descricoes.get(entry.getKey()), entry.getValue()))
                .toList();

        for (MovimentacaoEstoque movimentacao : movimentacoes) {
            boolean atualizado = switch (acao) {
                case RESERVAR -> pecaInsumoPortOut.reservar(movimentacao.id(), movimentacao.quantidade());
                case CONSUMIR -> pecaInsumoPortOut.baixar(movimentacao.id(), movimentacao.quantidade());
                case LIBERAR -> pecaInsumoPortOut.liberar(movimentacao.id(), movimentacao.quantidade());
            };
            String tipo = acao.name().toLowerCase();
            if (!atualizado) {
                log.warn("action=estoque_recusado tipo={} pecaId={} quantidade={}",
                        tipo, movimentacao.id(), movimentacao.quantidade());
                meterRegistry.counter("oficina.estoque.movimentacao.recusada", "tipo", tipo).increment();
                String motivo = acao == AcaoEstoque.RESERVAR
                        ? "Saldo insuficiente para reservar a peça "
                        : "Não há reserva suficiente para atualizar o estoque da peça ";
                throw new ApplicationException(409, motivo + movimentacao.descricao());
            }
            log.debug("action=estoque_movimentado tipo={} pecaId={} quantidade={}",
                    tipo, movimentacao.id(), movimentacao.quantidade());
            meterRegistry.counter("oficina.estoque.movimentacao.unidades", "tipo", tipo)
                    .increment(movimentacao.quantidade());
        }
    }

    private enum AcaoEstoque {
        RESERVAR,
        CONSUMIR,
        LIBERAR
    }

    private record MovimentacaoEstoque(Long id, String descricao, int quantidade) {
    }

    private void contarTransicao(StatusOrdemServico de, StatusOrdemServico para) {
        meterRegistry.counter("oficina.os.transicoes", "de", de.name(), "para", para.name()).increment();
    }

    private static BigDecimal valorTotal(OrdemServico os) {
        return Stream.concat(os.getItensServico().stream(), os.getItensPeca().stream())
                .map(item -> item.getSubtotal().valor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrdemServico buscar(Long id) {
        return ordemServicoPortOut.buscarPorId(id)
                .orElseThrow(() -> new ApplicationException(404, "Ordem de serviço não encontrada"));
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(clock);
    }
}
