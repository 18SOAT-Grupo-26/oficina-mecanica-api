package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Identidade;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identidade.ContatoRepository;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.identidade.IdentidadeRepository;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class IdentidadePayload {

    private final Logger log = LoggerFactory.getLogger(IdentidadePayload.class);

    private final IdentidadeRepository identidadeRepository;
    private final ContatoRepository contatoRepository;

    public IdentidadePayload(IdentidadeRepository identidadeRepository, ContatoRepository contatoRepository) {
        this.identidadeRepository = identidadeRepository;
        this.contatoRepository = contatoRepository;
    }

    public final void validarIdentidade(Identidade identidade) throws ApplicationException {
        if (identidade.contato() == null || identidade.contato().email() == null) {
            throw new ApplicationException(400, "Os dados de contato são obrigatórios");
        }
        if (!isEmailValido(identidade.contato().email())) {
            throw new ApplicationException(400, "E-mail inválido — deve conter '@' e um domínio válido");
        }
        if (identidade.nomeCompleto() == null || identidade.nomeCompleto().isBlank()) {
            throw new ApplicationException(400, "O nome completo é obrigatório");
        }
        if (identidade.primeiroNome() == null || identidade.primeiroNome().isBlank()) {
            throw new ApplicationException(400, "O primeiro nome é obrigatório");
        }
        // Documento: CPF ou CNPJ, com ou sem máscara; o Documento normaliza
        // e confere os dígitos verificadores, lançando 400 quando inválido.
        if (identidade.documento() == null || identidade.documento().isBlank()) {
            throw new ApplicationException(400, "O documento (CPF ou CNPJ) é obrigatório");
        }
        new Documento(identidade.documento());

        if (identidade.contato().telefone() == null
                || identidade.contato().telefone().isBlank()) {
            throw new ApplicationException(400, "O telefone é obrigatório");
        }
    }

    private boolean isEmailValido(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public void validarUnicidade(Identidade identidade) {
        if (contatoRepository.buscarIdentidadePorEmail(identidade.contato().email()).isPresent()) {
            throw new ApplicationException(409, "Já existe um usuário cadastrado com este e-mail");
        }

        if (identidadeRepository.existsByDocumento(new Documento(identidade.documento()).valor())) {
            throw new ApplicationException(409, "Já existe um usuário cadastrado com este documento");
        }

    }
}
