package br.com.fiap.oficinamecanicaapi.core.port.in.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;

public interface UsuarioUseCasePortIn {

    void cadastrar(Usuario usuario);

    DetalhesUsuario buscarPorIdentificador(String identificador);

}
