package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class UsuarioPayload {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public void validarUsuario(Usuario usuario) throws ApplicationException {
        if (usuario.nome() == null || usuario.nome().isBlank()) {
            throw new ApplicationException(400, "O nome é obrigatório");
        }
        if (usuario.email() == null || usuario.email().isBlank()) {
            throw new ApplicationException(400, "O e-mail é obrigatório");
        }
        if (!usuario.email().matches(EMAIL_REGEX)) {
            throw new ApplicationException(400, "E-mail inválido — deve conter '@' e um domínio válido");
        }
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
