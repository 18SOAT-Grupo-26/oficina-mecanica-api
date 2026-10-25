package br.com.fiap.oficinamecanicaapi.core.usecase.identidade.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.ContatoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Contato;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring", imports = {OffsetDateTime.class})
public interface ContatoMapper {

    Contato toResponse(ContatoEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(OffsetDateTime.now())")
    ContatoEntity toEntity(Contato contato);
}