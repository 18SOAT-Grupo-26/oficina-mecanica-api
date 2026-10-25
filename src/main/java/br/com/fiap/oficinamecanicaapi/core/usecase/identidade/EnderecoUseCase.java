package br.com.fiap.oficinamecanicaapi.core.usecase.identidade;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.EnderecoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Endereco;
import br.com.fiap.oficinamecanicaapi.core.port.in.identidade.EnderecoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.identidade.mapper.EnderecoMapper;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identidade.EnderecoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EnderecoUseCase implements EnderecoUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(EnderecoUseCase.class);

    private final EnderecoRepository enderecoRepository;
    private final EnderecoMapper enderecoMapper;

    public EnderecoUseCase(EnderecoRepository enderecoRepository, EnderecoMapper enderecoMapper) {
        this.enderecoRepository = enderecoRepository;
        this.enderecoMapper = enderecoMapper;
    }

    @Override
    public EnderecoEntity cadastrar(Endereco endereco) {
        log.info("Starting registration for address: {}", endereco);
        return enderecoRepository.save(enderecoMapper.toEntity(endereco));
    }
}
