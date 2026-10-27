package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrdemServicoJpaRepository extends JpaRepository<OrdemServicoEntity, Long> {

    /** Só OS que chegaram a FINALIZADA (inclusive as entregues); canceladas nunca passam por lá. */
    @Query("""
            select new br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico.PeriodoExecucao(
                inicio.dataCriacao, fim.dataCriacao)
            from HistoricoStatusOsEntity inicio, HistoricoStatusOsEntity fim
            where inicio.ordemServico = fim.ordemServico
              and inicio.statusAtualizado = br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.EM_EXECUCAO
              and fim.statusAtualizado = br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.FINALIZADA
            """)
    List<PeriodoExecucao> listarPeriodosExecucao();


    @Query("""
            select os
            from OrdemServicoEntity os
            where os.clienteId = :idCliente
            order by os.createdAt desc
            """)
    List<OrdemServicoEntity> listarOrdensByClientId(@Param("idCliente") Long idCliente);

}
