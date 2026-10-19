package br.com.fiap.oficinamecanicaapi.core.usecase.identity.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.AddressEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring", imports = {OffsetDateTime.class})
public interface AddressMapper {

    Address toResponse(AddressEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(OffsetDateTime.now())")
    AddressEntity toEntity(Address address);
}