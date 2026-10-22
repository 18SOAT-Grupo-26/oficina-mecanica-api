package br.com.fiap.oficinamecanicaapi.core.usecase.pecaInsumo.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.entity.PecaInsumoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PecaInsumoMapper {

    @Mapping(target = "criadoEm", source = "createdAt")
    @Mapping(target = "atualizadoEm", source = "updatedAt")
    PecaInsumoResponse toResponse(PecaInsumoEntity entity);
}
