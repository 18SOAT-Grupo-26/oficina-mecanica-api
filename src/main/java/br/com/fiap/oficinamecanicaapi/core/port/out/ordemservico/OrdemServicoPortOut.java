package br.com.fiap.oficinamecanicaapi.core.port.out.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServico;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface OrdemServicoPortOut {

    /** Grava a OS com itens e histórico e devolve a versão persistida, com os ids preenchidos. */
    OrdemServico salvar(OrdemServico ordemServico);

    Optional<OrdemServico> buscarPorId(Long id);

    /**
     * Tempo de EM_EXECUCAO até FINALIZADA de cada OS que já passou por FINALIZADA, inclusive as
     * já entregues. OS canceladas ou ainda em andamento ficam de fora.
     */
    List<Duration> listarTemposExecucao();
}
