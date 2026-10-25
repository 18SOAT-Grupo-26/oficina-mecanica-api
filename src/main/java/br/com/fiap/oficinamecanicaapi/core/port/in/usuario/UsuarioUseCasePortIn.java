package br.com.fiap.oficinamecanicaapi.core.port.in.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.identidade.entity.IdentidadeEntity;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;

public interface UsuarioUseCasePortIn {

    void cadastrar(Usuario usuario, IdentidadeEntity identidadeEntity);

    DetalhesUsuario buscarPorIdentificador(String identificador);

}
