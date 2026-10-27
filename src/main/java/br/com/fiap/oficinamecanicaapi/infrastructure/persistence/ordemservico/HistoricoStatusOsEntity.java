package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Um registro por mudança de status. Só recebe inserções: o histórico nunca é alterado.
 * Colunas combinadas com a Ana (dona das tabelas); se mudarem, o ajuste fica só aqui e na migration.
 */
@Entity
@Table(name = "historico_status_os")
@Getter
@Setter
public class HistoricoStatusOsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historico")
    private Long idHistorico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_os", nullable = false)
    private OrdemServicoEntity ordemServico;

    /**
     * Vazio só no registro da abertura (RECEBIDA).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_antigo", length = 30)
    private StatusOrdemServico statusAntigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_atualizado", nullable = false, length = 30)
    private StatusOrdemServico statusAtualizado;

    @Column(name = "data_criacao", nullable = false)
    private OffsetDateTime dataCriacao;
}
