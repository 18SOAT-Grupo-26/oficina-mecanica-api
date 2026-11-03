package br.com.fiap.oficinamecanicaapi.core.usecase.cliente.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    @Mapping(target = "documento", source = "documento.valor")
    @Mapping(target = "tipoDocumento", expression = "java(cliente.getDocumento().tipo())")
    ClienteResponse toResponse(Cliente cliente);
}
