package br.com.fiap.oficinamecanicaapi.core.port.out.usuario;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;

import java.util.Optional;

public interface UsuarioPortOut {

    /**
     * Grava um usuário novo e ativo; a senha já deve vir cifrada.
     */
    void cadastrar(Usuario usuario);

    Optional<DetalhesUsuario> buscarPorEmail(String email);

    Optional<DetalhesUsuario> buscarPorNomeUsuario(String nomeUsuario);

    boolean existePorEmail(String email);

    boolean existePorNomeUsuario(String nomeUsuario);

    boolean existePorClienteId(Long clienteId);

    boolean existePorPerfil(Perfil perfil);
}
