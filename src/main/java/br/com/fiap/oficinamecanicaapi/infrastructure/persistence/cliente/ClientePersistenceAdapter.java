package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class ClientePersistenceAdapter implements ClientePortOut {

    private final ClienteJpaRepository repository;
    private final ClientePersistenceMapper mapper;

    public ClientePersistenceAdapter(ClienteJpaRepository repository, ClientePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Cliente salvar(Cliente cliente) {
        return mapper.toDomain(repository.save(mapper.toEntity(cliente)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> buscarAtivoPorId(Long id) {
        return repository.findByIdAndAtivoTrue(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cliente> buscarAtivoPorDocumento(Documento documento) {
        return repository.findByDocumentoAndAtivoTrue(documento.valor()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorDocumento(Documento documento) {
        return repository.existsByDocumento(documento.valor());
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Cliente> listarAtivos(int pagina, int tamanho) {
        Page<ClienteEntity> page = repository.findAllByAtivoTrue(PageRequest.of(pagina, tamanho, Sort.by("id")));
        return new Pagina<>(page.map(mapper::toDomain).getContent(), pagina, tamanho, page.getTotalElements());
    }
}
