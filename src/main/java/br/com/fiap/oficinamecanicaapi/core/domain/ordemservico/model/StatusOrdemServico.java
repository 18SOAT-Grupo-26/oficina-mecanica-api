package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import java.util.Set;

/**
 * Status da Ordem de Serviço. Os seis primeiros vêm do enunciado; CANCELADA é premissa do grupo,
 * alcançada pela rejeição do orçamento ou pelo cancelamento da OS.
 *
 * <p>A OS só avança uma etapa por vez: não volta nem pula status. Pode ser cancelada até
 * AGUARDANDO_APROVACAO; a partir de EM_EXECUCAO, não. ENTREGUE e CANCELADA são finais.
 */
public enum StatusOrdemServico {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;

    public boolean podeTransitarPara(StatusOrdemServico destino) {
        return destino != null && destinosPermitidos().contains(destino);
    }

    public boolean isFinal() {
        return destinosPermitidos().isEmpty();
    }

    /**
     * A partir de EM_EXECUCAO a OS só avança de status; cancelada, não muda mais.
     */
    public boolean isEditavel() {
        return this == RECEBIDA || this == EM_DIAGNOSTICO || this == AGUARDANDO_APROVACAO;
    }

    private Set<StatusOrdemServico> destinosPermitidos() {
        return switch (this) {
            case RECEBIDA -> Set.of(EM_DIAGNOSTICO, CANCELADA);
            case EM_DIAGNOSTICO -> Set.of(AGUARDANDO_APROVACAO, CANCELADA);
            case AGUARDANDO_APROVACAO -> Set.of(EM_EXECUCAO, CANCELADA);
            case EM_EXECUCAO -> Set.of(FINALIZADA);
            case FINALIZADA -> Set.of(ENTREGUE);
            case ENTREGUE, CANCELADA -> Set.of();
        };
    }
}
