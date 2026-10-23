package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.entity.ServicoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.out.servico.ServicoPortOut;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.servico.ServicoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ServicoPersistenceAdapter implements ServicoPortOut {

    private final ServicoRepository repository;

    public ServicoPersistenceAdapter(ServicoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ServicoEntity salvar(ServicoEntity servico) {
        return repository.save(servico);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServicoEntity> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public void excluir(ServicoEntity servico) {
        repository.delete(servico);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorNomeNormalizado(String nomeNormalizado) {
        return repository.existsByNomeNormalizado(nomeNormalizado);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<ServicoEntity> listar(int pagina, int tamanho) {
        Page<ServicoEntity> page = repository.findAll(PageRequest.of(pagina, tamanho, Sort.by("id")));
        return new Pagina<>(page.getContent(), pagina, tamanho, page.getTotalElements());
    }
}
