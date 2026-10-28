package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.entity;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico.HistoricoStatusOsEntity;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico.ItemPecaOsEntity;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico.ItemServicoOsEntity;
import br.com.fiap.oficinamecanicaapi.shared.persistence.EntidadeComDatas;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Cliente e veículo ficam só como ids: a OS referencia os cadastros, não os carrega. Itens e
 * histórico pertencem à OS e são gravados junto com ela.
 */
@Entity
@Table(name = "ordem_servico")
@Getter
@Setter
public class OrdemServicoEntity extends EntidadeComDatas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "veiculo_id", nullable = false)
    private Long veiculoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrdemServico status;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemServicoOsEntity> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ItemPecaOsEntity> itensPeca = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("idHistorico")
    private List<HistoricoStatusOsEntity> historico = new ArrayList<>();

}
