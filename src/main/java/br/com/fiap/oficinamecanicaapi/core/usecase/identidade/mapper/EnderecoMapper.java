package br.com.fiap.oficinamecanicaapi.core.usecase.identidade.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.EnderecoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Endereco;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring", imports = {OffsetDateTime.class})
public interface EnderecoMapper {

    Endereco toResponse(EnderecoEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(OffsetDateTime.now())")
    EnderecoEntity toEntity(Endereco endereco);
}