package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServicoPersistenceMapper {

    @Mapping(target = "createdAt", source = "criadoEm")
    @Mapping(target = "updatedAt", source = "atualizadoEm")
    ServicoEntity toEntity(Servico servico);

    @Mapping(target = "criadoEm", source = "createdAt")
    @Mapping(target = "atualizadoEm", source = "updatedAt")
    Servico toDomain(ServicoEntity entity);
}
