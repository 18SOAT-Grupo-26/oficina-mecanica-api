package br.com.fiap.oficinamecanicaapi.core.usecase.identidade;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.ContatoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Contato;
import br.com.fiap.oficinamecanicaapi.core.port.in.identidade.ContatoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.identidade.mapper.ContatoMapper;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identidade.ContatoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContatoUseCase implements ContatoUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(ContatoUseCase.class);

    private final ContatoRepository contatoRepository;
    private final ContatoMapper contatoMapper;

    public ContatoUseCase(ContatoRepository contatoRepository, ContatoMapper contatoMapper) {
        this.contatoRepository = contatoRepository;
        this.contatoMapper = contatoMapper;
    }

    @Override
    public ContatoEntity cadastrar(Contato contato) {
        log.info("Starting registration for contact: {}", contato);
        return contatoRepository.save(contatoMapper.toEntity(contato));
    }
}
