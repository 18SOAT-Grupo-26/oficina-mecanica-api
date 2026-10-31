package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.out.servico.ServicoPortOut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ServicoPersistenceAdapter implements ServicoPortOut {

    private final ServicoJpaRepository repository;
    private final ServicoPersistenceMapper mapper;

    public ServicoPersistenceAdapter(ServicoJpaRepository repository, ServicoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Servico salvar(Servico servico) {
        return mapper.toDomain(repository.save(mapper.toEntity(servico)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Servico> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void excluir(Servico servico) {
        repository.deleteById(servico.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorNomeNormalizado(String nomeNormalizado) {
        return repository.existsByNomeNormalizado(nomeNormalizado);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Servico> listar(int pagina, int tamanho) {
        Page<ServicoEntity> page = repository.findAll(PageRequest.of(pagina - 1, tamanho, Sort.by("id")));
        return new Pagina<>(page.map(mapper::toDomain).getContent(), pagina, tamanho, page.getTotalElements());
    }
}
