package br.com.fiap.oficinamecanicaapi.core.usecase.veiculo.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Veiculo;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.VeiculoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VeiculoMapper {

    @Mapping(target = "placa", source = "placa.valor")
    VeiculoResponse toResponse(Veiculo veiculo);
}
