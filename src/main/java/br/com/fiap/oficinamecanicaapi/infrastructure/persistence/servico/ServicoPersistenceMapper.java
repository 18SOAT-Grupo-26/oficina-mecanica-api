package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServicoPersistenceMapper {

    @Mapping(target = "createdAt", source = "criadoEm")
    @Mapping(target = "updatedAt", source = "atualizadoEm")
    ServicoEntity toEntity(Servico servico);

    /**
     * O modelo não tem setters; a reconstrução passa pela fábrica do domínio.
     */
    default Servico toDomain(ServicoEntity entity) {
        return Servico.reconstituir(
                entity.getId(),
                entity.getNome(),
                entity.getNomeNormalizado(),
                entity.getDescricao(),
                entity.getPreco(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
