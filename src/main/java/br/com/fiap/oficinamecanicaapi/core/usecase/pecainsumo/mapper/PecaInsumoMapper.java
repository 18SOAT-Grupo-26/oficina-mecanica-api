package br.com.fiap.oficinamecanicaapi.core.usecase.pecainsumo.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model.PecaInsumoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PecaInsumoMapper {

    PecaInsumoResponse toResponse(PecaInsumo pecaInsumo);
}
