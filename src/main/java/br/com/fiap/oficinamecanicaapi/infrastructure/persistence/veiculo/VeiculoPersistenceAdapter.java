package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.veiculo;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.entity.VeiculoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Placa;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Veiculo;
import br.com.fiap.oficinamecanicaapi.core.port.out.veiculo.VeiculoPortOut;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.cliente.ClienteJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class VeiculoPersistenceAdapter implements VeiculoPortOut {

    private final VeiculoJpaRepository repository;
    private final ClienteJpaRepository clienteRepository;
    private final VeiculoPersistenceMapper mapper;

    public VeiculoPersistenceAdapter(VeiculoJpaRepository repository, ClienteJpaRepository clienteRepository,
                                     VeiculoPersistenceMapper mapper) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Veiculo salvar(Veiculo veiculo) {
        VeiculoEntity entity = mapper.toEntity(veiculo);
        // referência sem SELECT; a existência do cliente já foi validada no caso de uso
        entity.setCliente(clienteRepository.getReferenceById(veiculo.getClienteId()));
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Veiculo> buscarAtivoPorId(Long id) {
        return repository.findByIdAndAtivoTrue(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Veiculo> buscarAtivoPorPlaca(Placa placa) {
        return repository.findByPlacaAndAtivoTrue(placa.valor()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existePorPlaca(Placa placa) {
        return repository.existsByPlaca(placa.valor());
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Veiculo> listarAtivos(Long clienteId, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("id"));
        Page<VeiculoEntity> page = clienteId == null
                ? repository.findAllByAtivoTrue(pageable)
                : repository.findAllByClienteIdAndAtivoTrue(clienteId, pageable);
        return new Pagina<>(page.map(mapper::toDomain).getContent(), pagina, tamanho, page.getTotalElements());
    }
}
