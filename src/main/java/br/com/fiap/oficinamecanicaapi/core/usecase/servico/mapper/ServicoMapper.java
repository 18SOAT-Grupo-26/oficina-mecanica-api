package br.com.fiap.oficinamecanicaapi.core.usecase.servico.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServicoMapper {

    ServicoResponse toResponse(Servico servico);
}
