package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.core.domain.user.model.User;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class UserPayload {

    public void validateUserPayload(User userRequest) throws ApplicationException {
        if (userRequest.username() == null || userRequest.username().isBlank()) {
            throw new ApplicationException(400, "Username is required");
        }
        if (userRequest.changeRequired()) {
            throw new ApplicationException(400, "Change required is required");
        }
    }

    public boolean isEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }

}
