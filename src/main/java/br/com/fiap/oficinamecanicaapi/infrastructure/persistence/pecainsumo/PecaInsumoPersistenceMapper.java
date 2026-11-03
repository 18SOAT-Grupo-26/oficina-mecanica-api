package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecainsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model.PecaInsumo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PecaInsumoPersistenceMapper {

    @Mapping(target = "createdAt", source = "criadoEm")
    @Mapping(target = "updatedAt", source = "atualizadoEm")
    PecaInsumoEntity toEntity(PecaInsumo pecaInsumo);

    /**
     * O modelo não tem setters; a reconstrução passa pela fábrica do domínio.
     */
    default PecaInsumo toDomain(PecaInsumoEntity entity) {
        return PecaInsumo.reconstituir(
                entity.getId(),
                entity.getNome(),
                entity.getNomeNormalizado(),
                entity.getDescricao(),
                entity.getQuantidade(),
                entity.getReservado(),
                entity.getPreco(),
                entity.isAtivo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
