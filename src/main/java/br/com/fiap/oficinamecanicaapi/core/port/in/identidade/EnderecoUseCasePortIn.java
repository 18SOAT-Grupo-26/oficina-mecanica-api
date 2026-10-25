package br.com.fiap.oficinamecanicaapi.core.port.in.identidade;

import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.EnderecoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Endereco;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface EnderecoUseCasePortIn {

    EnderecoEntity cadastrar(@NotNull @Valid Endereco endereco);

}
