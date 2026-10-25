package br.com.fiap.oficinamecanicaapi.core.port.in.identidade;


import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.ContatoEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.model.Contato;

public interface ContatoUseCasePortIn {

    ContatoEntity cadastrar(Contato contato);

}
