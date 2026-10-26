package br.com.fiap.oficinamecanicaapi.core.usecase.ordemservico.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServico;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.ValorMonetario;
import org.mapstruct.Mapper;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrdemServicoMapper {

    OrdemServicoResponse toResponse(OrdemServico ordemServico);

    default BigDecimal toBigDecimal(ValorMonetario valor) {
        return valor == null ? null : valor.valor();
    }
}
