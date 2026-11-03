package br.com.fiap.oficinamecanicaapi.core.port.in.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;

public interface UsuarioUseCasePortIn {

    void cadastrar(Usuario usuario);

    DetalhesUsuario buscarPorIdentificador(String identificador);

    boolean existeAdministrador();

}
