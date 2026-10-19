package br.com.fiap.oficinamecanicaapi.core.usecase.identity.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.ContactEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Contact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring", imports = {OffsetDateTime.class})
public interface ContactMapper {

    Contact toResponse(ContactEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(OffsetDateTime.now())")
    ContactEntity toEntity(Contact contact);
}