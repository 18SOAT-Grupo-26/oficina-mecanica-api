package br.com.fiap.oficinamecanicaapi.core.usecase.identidade.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.ContatoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.EnderecoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.IdentidadeEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Identidade;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;

@Mapper(
        componentModel = "spring",
        imports = {Documento.class, OffsetDateTime.class},
        uses = {
                ContatoMapper.class,
                EnderecoMapper.class
        }
)
public interface IdentidadeMapper {

    Identidade toResponse(IdentidadeEntity entity);

    @Mapping(target = "documento", expression = "java(new Documento(identidade.documento()).valor())")
    @Mapping(target = "tipoDocumento", expression = "java(new Documento(identidade.documento()).tipo())")
    IdentidadeEntity toEntity(Identidade identidade);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "contato", source = "contatoEntity")
    @Mapping(target = "endereco", source = "enderecoEntity")
    @Mapping(target = "ativo", constant = "true")
    @Mapping(target = "documento", expression = "java(new Documento(identidade.documento()).valor())")
    @Mapping(target = "tipoDocumento", expression = "java(new Documento(identidade.documento()).tipo())")
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(OffsetDateTime.now())")
    IdentidadeEntity toEntityCadastro(
            Identidade identidade,
            ContatoEntity contatoEntity,
            EnderecoEntity enderecoEntity
    );
}
