package br.com.fiap.oficinamecanicaapi.core.domain.user.model;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ADMINISTRATOR,
    MANAGER,
    OPERATOR;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name(); // padrão Spring Security
    }
}
