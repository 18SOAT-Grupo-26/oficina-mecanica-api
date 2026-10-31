package br.com.fiap.oficinamecanicaapi.core.usecase.ordemservico.mapper;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServico;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.ValorMonetario;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoResponse;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface OrdemServicoMapper {

    OrdemServicoResponse toResponse(OrdemServico ordemServico);

    default BigDecimal toBigDecimal(ValorMonetario valor) {
        return valor == null ? null : valor.valor();
    }

    List<OrdemServicoResponse> toResponseList(List<OrdemServico> ordemServicos);
}
