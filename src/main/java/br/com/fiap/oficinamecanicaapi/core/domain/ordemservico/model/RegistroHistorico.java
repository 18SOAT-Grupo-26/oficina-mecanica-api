package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

import java.time.OffsetDateTime;

/**
 * Objeto de valor de uma mudança de status da OS: de qual status saiu, para qual foi e quando.
 * Só o registro da abertura (RECEBIDA) não tem status antigo.
 */
public record RegistroHistorico(StatusOrdemServico statusAntigo, StatusOrdemServico statusAtualizado,
                                OffsetDateTime dataHora) {

    public RegistroHistorico {
        if (statusAtualizado == null || dataHora == null) {
            throw new ApplicationException(400, "O registro de histórico exige status atualizado e data/hora");
        }
        if (statusAntigo == null && statusAtualizado != StatusOrdemServico.RECEBIDA) {
            throw new ApplicationException(400, "O registro de histórico exige o status antigo, exceto na abertura");
        }
    }
}
