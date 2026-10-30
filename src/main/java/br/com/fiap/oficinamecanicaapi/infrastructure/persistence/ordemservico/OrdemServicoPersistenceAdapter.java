package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServico;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.out.ordemservico.OrdemServicoPortOut;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class OrdemServicoPersistenceAdapter implements OrdemServicoPortOut {

    private final OrdemServicoJpaRepository repository;
    private final OrdemServicoPersistenceMapper mapper;

    public OrdemServicoPersistenceAdapter(OrdemServicoJpaRepository repository, OrdemServicoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * O flush grava já os itens e registros novos, para o agregado devolvido vir com os ids.
     */
    @Override
    @Transactional
    public OrdemServico salvar(OrdemServico ordemServico) {
        OrdemServicoEntity entity = ordemServico.getId() == null
                ? mapper.toEntity(ordemServico)
                : carregarParaAtualizar(ordemServico);
        return mapper.toDomain(repository.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrdemServico> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Duration> listarTemposExecucao() {
        return repository.listarPeriodosExecucao().stream()
                .map(periodo -> Duration.between(periodo.inicio(), periodo.fim()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdemServico> listarDoCliente(Long idCliente) {
        return repository.listarOrdensByClientId(idCliente).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * O id desempata OS abertas no mesmo instante, para a paginação não repetir nem pular registros.
     */
    @Override
    @Transactional(readOnly = true)
    public Pagina<OrdemServico> listar(Long idCliente, StatusOrdemServico status, int pagina, int tamanho) {
        PageRequest pageable = PageRequest.of(pagina - 1, tamanho, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<OrdemServicoEntity> page = repository.listar(idCliente, status, pageable);
        return new Pagina<>(page.map(mapper::toDomain).getContent(), pagina, tamanho, page.getTotalElements());
    }

    private OrdemServicoEntity carregarParaAtualizar(OrdemServico ordemServico) {
        OrdemServicoEntity entity = repository.findById(ordemServico.getId())
                .orElseThrow(() -> new ApplicationException(404, "Ordem de serviço não encontrada"));
        mapper.copiar(ordemServico, entity);
        return entity;
    }
}
