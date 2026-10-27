package br.com.fiap.oficinamecanicaapi.core.usecase.servico.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.entity.ServicoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServicoMapper {

    @Mapping(target = "criadoEm", source = "createdAt")
    @Mapping(target = "atualizadoEm", source = "updatedAt")
    ServicoResponse toResponse(ServicoEntity entity);
}
