package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class UsuarioPayload {

    public void validarUsuario(Usuario usuario) throws ApplicationException {
        if (usuario.nomeUsuario() == null || usuario.nomeUsuario().isBlank()) {
            throw new ApplicationException(400, "O nome de usuário é obrigatório");
        }
        if (usuario.trocaSenhaObrigatoria()) {
            throw new ApplicationException(400, "O campo trocaSenhaObrigatoria é obrigatório");
        }
    }

    public boolean isEmail(String identificador) {
        return identificador != null && identificador.contains("@");
    }

}
