package br.com.fiap.oficinamecanicaapi.core.usecase.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.entity.PecaInsumoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.pecaInsumo.PecaInsumoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.infrastructure.persistence.repository.pecaInsumo.PecaInsumoRepository;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

@Service
public class PecaInsumoUseCase implements PecaInsumoUseCasePortIn {

    private static final int NOME_MIN_LENGTH = 3;
    private static final int DESCRICAO_MAX_LENGTH = 500;
    private static final int PRECO_MAX_CASAS_DECIMAIS = 2;

    private final PecaInsumoRepository repository;

    public PecaInsumoUseCase(PecaInsumoRepository repository) {
        this.repository = repository;
    }

    @Override
    public PecaInsumoResponse criarInsumo(PecaInsumoRequest request) throws ApplicationException {
        String nome = request.nome() == null ? null : request.nome().trim();
        if (nome == null || nome.isEmpty()) {
            throw new ApplicationException(422, "O campo nome é obrigatório");
        }
        if (nome.length() < NOME_MIN_LENGTH) {
            throw new ApplicationException(422, "O nome deve ter no mínimo 3 caracteres");
        }

        if (request.quantidade() == null) {
            throw new ApplicationException(422, "O campo quantidade é obrigatório");
        }
        if (request.quantidade() < 0) {
            throw new ApplicationException(422, "Quantidade inválida");
        }

        if (request.preco() == null) {
            throw new ApplicationException(422, "O campo preco é obrigatório");
        }
        if (request.preco().signum() <= 0
                || request.preco().stripTrailingZeros().scale() > PRECO_MAX_CASAS_DECIMAIS) {
            throw new ApplicationException(422, "Preço inválido");
        }

        int reservado = request.reservado() == null ? 0 : request.reservado();
        if (reservado < 0 || reservado > request.quantidade()) {
            throw new ApplicationException(422,
                    "Quantidade reservada inválida. Deve ser entre 0 e a quantidade em estoque");
        }

        if (request.descricao() != null && request.descricao().length() > DESCRICAO_MAX_LENGTH) {
            throw new ApplicationException(422, "A descrição deve ter no máximo 500 caracteres");
        }

        String nomeNormalizado = normalizarNome(nome);
        if (repository.existsByNomeNormalizado(nomeNormalizado)) {
            throw new ApplicationException(409, "Já existe um insumo cadastrado com este nome");
        }

        PecaInsumoEntity entity = new PecaInsumoEntity();
        entity.setNome(nome);
        entity.setNomeNormalizado(nomeNormalizado);
        entity.setDescricao(request.descricao());
        entity.setQuantidade(request.quantidade());
        entity.setReservado(reservado);
        entity.setPreco(request.preco().setScale(PRECO_MAX_CASAS_DECIMAIS));

        PecaInsumoEntity saved = repository.save(entity);

        return new PecaInsumoResponse(
                saved.getId(),
                saved.getNome(),
                saved.getDescricao(),
                saved.getQuantidade(),
                saved.getReservado(),
                saved.getPreco(),
                saved.getCreatedAt(),
                saved.getUpdatedAt());
    }

    /** Ignora acentos, maiúsculas/minúsculas e espaços extras. */
    private static String normalizarNome(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
