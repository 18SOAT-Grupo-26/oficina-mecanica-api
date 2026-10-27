package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import lombok.Getter;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.*;

/**
 * Agregado raiz da Ordem de Serviço. O status só muda pelos comandos de negócio, que validam a
 * transição em {@link StatusOrdemServico} e gravam cada mudança no histórico. A data/hora de cada
 * comando vem de fora, para que o domínio não dependa do relógio.
 *
 * <p>Os itens (serviços e peças, com o preço congelado na inclusão) são da OS e alimentam o
 * orçamento. Cálculo, envio e decisão do orçamento ficam com o contexto de orçamento (P3); a OS só
 * reage aos fatos dele: orçamento enviado → {@link #aguardarAprovacao}, aprovado →
 * {@link #iniciarExecucao}, rejeitado → {@link #cancelar}.
 */
public class OrdemServico {

    @Getter
    private final Long id;
    /**
     * Cliente e veículo são definidos na abertura e não mudam: se estiverem errados, cancela-se a OS.
     */
    @Getter
    private final Long idCliente;
    @Getter
    private final Long idVeiculo;
    @Getter
    private StatusOrdemServico status;
    private final List<ItemServico> itensServico;
    private final List<ItemPeca> itensPeca;
    private final List<RegistroHistorico> historico;

    private OrdemServico(Long id, Long idCliente, Long idVeiculo, StatusOrdemServico status,
                         List<ItemServico> itensServico, List<ItemPeca> itensPeca,
                         List<RegistroHistorico> historico) {
        this.id = id;
        this.idCliente = idCliente;
        this.idVeiculo = idVeiculo;
        this.status = status;
        this.itensServico = new ArrayList<>(itensServico);
        this.itensPeca = new ArrayList<>(itensPeca);
        this.historico = new ArrayList<>(historico);
    }

    /**
     * Abre uma OS nova, ainda não persistida, para o cliente e o veículo. Toda OS aberta entra como
     * RECEBIDA e, como já tem cliente e veículo, vai sozinha para EM_DIAGNOSTICO no mesmo momento: o
     * histórico registra as duas mudanças.
     */
    public static OrdemServico abrir(Long idCliente, Long idVeiculo, OffsetDateTime momento) {
        exigirMomento(momento);
        exigirCliente(idCliente);
        exigirVeiculo(idVeiculo);
        OrdemServico os = new OrdemServico(null, idCliente, idVeiculo, RECEBIDA, List.of(), List.of(),
                List.of(new RegistroHistorico(null, RECEBIDA, momento)));
        os.registrarTransicao(EM_DIAGNOSTICO, momento);
        return os;
    }

    /**
     * Reconstrói uma OS já persistida (uso exclusivo dos adapters de saída).
     */
    public static OrdemServico reconstituir(Long id, Long idCliente, Long idVeiculo, StatusOrdemServico status,
                                            List<ItemServico> itensServico, List<ItemPeca> itensPeca,
                                            List<RegistroHistorico> historico) {
        return new OrdemServico(id, idCliente, idVeiculo, status, itensServico, itensPeca, historico);
    }

    // ---- edição ----------------------------------------------------------------------------------

    /**
     * Recusa editar a OS a partir de EM_EXECUCAO (ou cancelada), mesmo sem mudança nenhuma. Antes
     * disso, só os itens mudam, e só até EM_DIAGNOSTICO.
     */
    public void exigirEditavel() {
        if (!status.isEditavel()) {
            throw new ApplicationException(409, "A OS não pode ser editada com status " + status);
        }
    }

    // ---- itens -----------------------------------------------------------------------------------

    public void incluirServico(Long idServico, String descricao, ValorMonetario precoUnitario, int quantidade) {
        exigirItensEditaveis();
        if (buscarServico(idServico).isPresent()) {
            throw new ApplicationException(409, "Serviço já incluído na OS");
        }
        itensServico.add(ItemServico.novo(idServico, descricao, precoUnitario, quantidade));
    }

    public void incluirPeca(Long idPeca, String descricao, ValorMonetario precoUnitario, int quantidade) {
        exigirItensEditaveis();
        if (buscarPeca(idPeca).isPresent()) {
            throw new ApplicationException(409, "Peça já incluída na OS");
        }
        itensPeca.add(ItemPeca.novo(idPeca, descricao, precoUnitario, quantidade));
    }

    public void removerServico(Long idServico) {
        exigirItensEditaveis();
        ItemServico item = buscarServico(idServico)
                .orElseThrow(() -> new ApplicationException(404, "Serviço não encontrado na OS"));
        itensServico.remove(item);
    }

    public void removerPeca(Long idPeca) {
        exigirItensEditaveis();
        ItemPeca item = buscarPeca(idPeca)
                .orElseThrow(() -> new ApplicationException(404, "Peça não encontrada na OS"));
        itensPeca.remove(item);
    }

    // ---- status ----------------------------------------------------------------------------------

    /**
     * O orçamento foi enviado ao cliente: a OS aguarda a decisão dele. Sem itens, não há o que orçar.
     */
    public void aguardarAprovacao(OffsetDateTime momento) {
        exigirTransicaoPara(AGUARDANDO_APROVACAO, momento);
        if (itensServico.isEmpty() && itensPeca.isEmpty()) {
            throw new ApplicationException(409, "A OS precisa de ao menos um item para gerar orçamento");
        }
        registrarTransicao(AGUARDANDO_APROVACAO, momento);
    }

    /**
     * O cliente aprovou o orçamento: isso já libera o mecânico, e a OS vai para EM_EXECUCAO.
     */
    public void iniciarExecucao(OffsetDateTime momento) {
        exigirTransicaoPara(EM_EXECUCAO, momento);
        registrarTransicao(EM_EXECUCAO, momento);
    }

    /**
     * O cliente rejeitou o orçamento: só existe orçamento a rejeitar em AGUARDANDO_APROVACAO.
     */
    public void rejeitarOrcamento(OffsetDateTime momento) {
        exigirMomento(momento);
        if (status != AGUARDANDO_APROVACAO) {
            throw new ApplicationException(409, "Só é possível rejeitar o orçamento com a OS em AGUARDANDO_APROVACAO");
        }
        cancelar(momento);
    }

    /**
     * Cancela a OS sem removê-la, pela rejeição do orçamento ou por decisão da oficina. Vale até
     * AGUARDANDO_APROVACAO (a partir de EM_EXECUCAO, não); liberar reservas é com quem chama.
     */
    public void cancelar(OffsetDateTime momento) {
        exigirTransicaoPara(CANCELADA, momento);
        registrarTransicao(CANCELADA, momento);
    }

    public void finalizar(OffsetDateTime momento) {
        exigirTransicaoPara(FINALIZADA, momento);
        registrarTransicao(FINALIZADA, momento);
    }

    public void entregar(OffsetDateTime momento) {
        exigirTransicaoPara(ENTREGUE, momento);
        registrarTransicao(ENTREGUE, momento);
    }

    // ---- consultas -------------------------------------------------------------------------------

    public List<ItemServico> getItensServico() {
        return Collections.unmodifiableList(itensServico);
    }

    public List<ItemPeca> getItensPeca() {
        return Collections.unmodifiableList(itensPeca);
    }

    public List<RegistroHistorico> getHistorico() {
        return Collections.unmodifiableList(historico);
    }

    /**
     * Tempo de EM_EXECUCAO até FINALIZADA; vazio enquanto a OS não passou pelos dois status.
     */
    public Optional<Duration> getTempoExecucao() {
        Optional<OffsetDateTime> inicio = momentoDoStatus(EM_EXECUCAO);
        Optional<OffsetDateTime> fim = momentoDoStatus(FINALIZADA);
        if (inicio.isEmpty() || fim.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(inicio.get(), fim.get()));
    }

    // ---- regras internas -------------------------------------------------------------------------

    private void exigirTransicaoPara(StatusOrdemServico destino, OffsetDateTime momento) {
        exigirMomento(momento);
        if (!status.podeTransitarPara(destino)) {
            throw new ApplicationException(409, "Transição inválida: " + status + " → " + destino);
        }
    }

    private void registrarTransicao(StatusOrdemServico destino, OffsetDateTime momento) {
        historico.add(new RegistroHistorico(status, destino, momento));
        this.status = destino;
    }

    /**
     * Depois do envio do orçamento as peças já estão reservadas, então os itens ficam fixos.
     */
    private void exigirItensEditaveis() {
        if (status != RECEBIDA && status != EM_DIAGNOSTICO) {
            throw new ApplicationException(409, "Itens só podem ser alterados com a OS em RECEBIDA ou EM_DIAGNOSTICO");
        }
    }

    private Optional<ItemServico> buscarServico(Long idServico) {
        return itensServico.stream().filter(item -> item.getIdServico().equals(idServico)).findFirst();
    }

    private Optional<ItemPeca> buscarPeca(Long idPeca) {
        return itensPeca.stream().filter(item -> item.getIdPeca().equals(idPeca)).findFirst();
    }

    private Optional<OffsetDateTime> momentoDoStatus(StatusOrdemServico alvo) {
        return historico.stream()
                .filter(registro -> registro.statusAtualizado() == alvo)
                .map(RegistroHistorico::dataHora)
                .findFirst();
    }

    private static void exigirCliente(Long idCliente) {
        if (idCliente == null) {
            throw new ApplicationException(400, "O cliente da OS é obrigatório");
        }
    }

    private static void exigirVeiculo(Long idVeiculo) {
        if (idVeiculo == null) {
            throw new ApplicationException(400, "O veículo da OS é obrigatório");
        }
    }

    private static void exigirMomento(OffsetDateTime momento) {
        if (momento == null) {
            throw new ApplicationException(400, "A data/hora da operação é obrigatória");
        }
    }
}
