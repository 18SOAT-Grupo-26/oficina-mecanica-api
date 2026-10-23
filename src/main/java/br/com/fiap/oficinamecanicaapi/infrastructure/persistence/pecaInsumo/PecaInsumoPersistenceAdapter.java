package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.entity.PecaInsumoEntity;
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

    public PecaInsumoPersistenceAdapter(PecaInsumoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public PecaInsumoEntity salvar(PecaInsumoEntity pecaInsumo) {
        return repository.save(pecaInsumo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PecaInsumoEntity> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PecaInsumoEntity> buscarPorIds(Collection<Long> ids) {
        return repository.findAllById(ids);
    }

    @Override
    @Transactional
    public void excluir(PecaInsumoEntity pecaInsumo) {
        repository.delete(pecaInsumo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorNomeNormalizado(String nomeNormalizado) {
        return repository.existsByNomeNormalizado(nomeNormalizado);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<PecaInsumoEntity> listar(int pagina, int tamanho) {
        Page<PecaInsumoEntity> page = repository.findAll(PageRequest.of(pagina, tamanho, Sort.by("id")));
        return new Pagina<>(page.getContent(), pagina, tamanho, page.getTotalElements());
    }
}
