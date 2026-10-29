package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PecaInsumoPersistenceMapper {

    @Mapping(target = "createdAt", source = "criadoEm")
    @Mapping(target = "updatedAt", source = "atualizadoEm")
    PecaInsumoEntity toEntity(PecaInsumo pecaInsumo);

    @Mapping(target = "criadoEm", source = "createdAt")
    @Mapping(target = "atualizadoEm", source = "updatedAt")
    PecaInsumo toDomain(PecaInsumoEntity entity);
}
