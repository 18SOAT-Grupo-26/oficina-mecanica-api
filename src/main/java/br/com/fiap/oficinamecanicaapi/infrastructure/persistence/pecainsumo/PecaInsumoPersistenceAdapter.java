package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecainsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.out.pecainsumo.PecaInsumoPortOut;
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

    private final PecaInsumoJpaRepository repository;
    private final PecaInsumoPersistenceMapper mapper;

    public PecaInsumoPersistenceAdapter(PecaInsumoJpaRepository repository, PecaInsumoPersistenceMapper mapper) {
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
    public Optional<PecaInsumo> buscarAtivoPorId(Long id) {
        return repository.findByIdAndAtivoTrue(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PecaInsumo> buscarPorIds(Collection<Long> ids) {
        return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorNomeNormalizado(String nomeNormalizado) {
        return repository.existsByNomeNormalizado(nomeNormalizado);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<PecaInsumo> listarAtivos(int pagina, int tamanho) {
        Page<PecaInsumoEntity> page = repository.findAllByAtivoTrue(PageRequest.of(pagina - 1, tamanho, Sort.by("id")));
        return new Pagina<>(page.map(mapper::toDomain).getContent(), pagina, tamanho, page.getTotalElements());
    }
}
