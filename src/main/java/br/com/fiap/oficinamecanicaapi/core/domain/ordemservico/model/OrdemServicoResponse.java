package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrdemServicoResponse(
        Long id,
        Long idCliente,
        Long idVeiculo,
        StatusOrdemServico status,
        List<ItemServico> itensServico,
        List<ItemPeca> itensPeca,
        List<RegistroHistorico> historico
) {

    public record ItemServico(Long id, Long idServico, String descricao, BigDecimal precoUnitario,
                              Integer quantidade, BigDecimal subtotal) {
    }

    public record ItemPeca(Long id, Long idPeca, String descricao, BigDecimal precoUnitario,
                           Integer quantidade, BigDecimal subtotal) {
    }

    /**
     * {@code statusAntigo} vem vazio só no registro da abertura.
     */
    public record RegistroHistorico(StatusOrdemServico statusAntigo, StatusOrdemServico statusAtualizado,
                                    OffsetDateTime dataHora) {
    }
}
