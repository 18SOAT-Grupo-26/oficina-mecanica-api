package br.com.fiap.oficinamecanicaapi.core.usecase.pecaInsumo.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PecaInsumoMapper {

    PecaInsumoResponse toResponse(PecaInsumo pecaInsumo);
}
