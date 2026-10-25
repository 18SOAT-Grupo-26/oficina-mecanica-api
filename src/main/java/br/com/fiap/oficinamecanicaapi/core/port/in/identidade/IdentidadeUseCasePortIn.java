package br.com.fiap.oficinamecanicaapi.core.port.in.identidade;


import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.IdentidadeEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Identidade;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;

public interface IdentidadeUseCasePortIn {

    IdentidadeEntity cadastrar(Identidade identidade) throws ApplicationException;

}