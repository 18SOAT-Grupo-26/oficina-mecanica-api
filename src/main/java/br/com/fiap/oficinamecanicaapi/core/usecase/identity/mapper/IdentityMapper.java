package br.com.fiap.oficinamecanicaapi.core.usecase.identity.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.AddressEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.ContactEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.entity.IdentityEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identity.model.Identity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;

@Mapper(
        componentModel = "spring",
        imports = {Documento.class, OffsetDateTime.class},
        uses = {
                ContactMapper.class,
                AddressMapper.class
        }
)
public interface IdentityMapper {

    Identity toResponse(IdentityEntity entity);

    @Mapping(target = "documento", expression = "java(new Documento(identity.documento()).valor())")
    @Mapping(target = "tipoDocumento", expression = "java(new Documento(identity.documento()).tipo())")
    IdentityEntity toEntity(Identity identity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contact", source = "contactEntity")
    @Mapping(target = "address", source = "addressEntity")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "documento", expression = "java(new Documento(identity.documento()).valor())")
    @Mapping(target = "tipoDocumento", expression = "java(new Documento(identity.documento()).tipo())")
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(OffsetDateTime.now())")
    IdentityEntity persistIdentityWithContactAndAddressEntities(
            Identity identity,
            ContactEntity contactEntity,
            AddressEntity addressEntity
    );
}
