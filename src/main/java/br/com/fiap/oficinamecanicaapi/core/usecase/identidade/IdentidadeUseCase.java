package br.com.fiap.oficinamecanicaapi.core.usecase.identidade;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.IdentidadeEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Identidade;
import br.com.fiap.oficinamecanicaapi.core.port.in.identidade.EnderecoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.identidade.ContatoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.identidade.IdentidadeUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.identidade.mapper.IdentidadeMapper;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identidade.IdentidadeRepository;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import br.com.fiap.oficinamecanicaapi.shared.utils.helpers.IdentidadePayload;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentidadeUseCase implements IdentidadeUseCasePortIn {

    private static final Logger log = LoggerFactory.getLogger(IdentidadeUseCase.class);

    private final IdentidadeRepository identidadeRepository;
    private final EnderecoUseCasePortIn enderecoService;
    private final ContatoUseCasePortIn contatoService;
    private final IdentidadePayload identidadePayload;
    private final IdentidadeMapper identidadeMapper;

    public IdentidadeUseCase(IdentidadeRepository identidadeRepository,
                             EnderecoUseCasePortIn enderecoService,
                             ContatoUseCasePortIn contatoService,
                             IdentidadePayload identidadePayload,
                             IdentidadeMapper identidadeMapper,
                             MeterRegistry meterRegistry) {
        this.identidadeRepository = identidadeRepository;
        this.enderecoService = enderecoService;
        this.contatoService = contatoService;
        this.identidadePayload = identidadePayload;
        this.identidadeMapper = identidadeMapper;
    }

    @Override
    @Transactional
    public IdentidadeEntity cadastrar(Identidade identidade) throws ApplicationException {
        log.info("Validating payloads: {}", identidade);
        validarPayloads(identidade);

        log.info("Starting register on database: {}", identidade);
        return salvar(identidade);
    }

    private IdentidadeEntity salvar(Identidade identidade) {
        var contato = contatoService.cadastrar(identidade.contato());

        var endereco = enderecoService.cadastrar(identidade.endereco());

        return identidadeRepository.save(
                        identidadeMapper.toEntityCadastro(identidade, contato, endereco));
    }

    private void validarPayloads(Identidade identidade) {
        log.info("Validating identity payload: {}", identidade);
        identidadePayload.validarIdentidade(identidade);
        log.info("Validating identity uniqueness: {}", identidade);
        identidadePayload.validarUnicidade(identidade);
    }
}
