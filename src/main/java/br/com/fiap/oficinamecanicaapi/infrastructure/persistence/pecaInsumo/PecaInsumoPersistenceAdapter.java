package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.out.pecaInsumo.PecaInsumoPortOut;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.pecaInsumo.PecaInsumoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class PecaInsumoPersistenceAdapter implements PecaInsumoPortOut {

    private final PecaInsumoRepository repository;
    private final PecaInsumoPersistenceMapper mapper;

    public PecaInsumoPersistenceAdapter(PecaInsumoRepository repository, PecaInsumoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public PecaInsumo salvar(PecaInsumo pecaInsumo) {
        return mapper.toDomain(repository.save(mapper.toEntity(pecaInsumo)));
    }

    @Override
    @Transactional
    public boolean reservar(Long id, int quantidade) {
        return repository.reservar(id, quantidade) == 1;
    }

    @Override
    @Transactional
    public boolean liberar(Long id, int quantidade) {
        return repository.liberar(id, quantidade) == 1;
    }

    @Override
    @Transactional
    public boolean baixar(Long id, int quantidade) {
        return repository.baixar(id, quantidade) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PecaInsumo> buscarPorId(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PecaInsumo> buscarPorIds(Collection<Long> ids) {
        return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void excluir(PecaInsumo pecaInsumo) {
        repository.deleteById(pecaInsumo.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorNomeNormalizado(String nomeNormalizado) {
        return repository.existsByNomeNormalizado(nomeNormalizado);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<PecaInsumo> listar(int pagina, int tamanho) {
        Page<PecaInsumoEntity> page = repository.findAll(PageRequest.of(pagina, tamanho, Sort.by("id")));
        return new Pagina<>(page.map(mapper::toDomain).getContent(), pagina, tamanho, page.getTotalElements());
    }
}
