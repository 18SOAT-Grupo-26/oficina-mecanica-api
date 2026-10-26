package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Dados da OS, usados na abertura (POST) e na atualização completa (PUT). Todos os campos são
 * obrigatórios; {@code servicos} e {@code pecas} aceitam lista vazia, porque a OS abre antes do
 * diagnóstico e o PUT com lista vazia remove os itens daquele tipo. {@code documentoCliente}: CPF ou
 * CNPJ, com ou sem máscara. O status não muda por aqui: a mudança manual vai pelo PATCH com
 * {@link Status}, e o envio e a decisão do orçamento chegam pelo contexto de orçamento.
 */
public record OrdemServicoRequest(
        @NotBlank String documentoCliente,
        @NotNull Long idVeiculo,
        @NotNull List<@Valid Servico> servicos,
        @NotNull List<@Valid Peca> pecas
) {

    /** Serviço já cadastrado, pelo id, e a quantidade na OS. */
    public record Servico(@NotNull Long idServico, @NotNull @Positive Integer quantidade) {
    }

    /** Peça ou insumo já cadastrado, pelo id, e a quantidade na OS. */
    public record Peca(@NotNull Long idPeca, @NotNull @Positive Integer quantidade) {
    }

    /**
     * Body do PATCH de status: o status de destino, que precisa ser o próximo da sequência. Valem
     * FINALIZADA, ENTREGUE e CANCELADA (até AGUARDANDO_APROVACAO; a OS não é removida). EM_DIAGNOSTICO
     * vem da abertura; AGUARDANDO_APROVACAO e EM_EXECUCAO, do orçamento.
     */
    public record Status(@NotNull StatusOrdemServico status) {
    }
}
