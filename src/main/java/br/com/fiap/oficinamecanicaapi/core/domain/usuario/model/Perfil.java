package br.com.fiap.oficinamecanicaapi.core.domain.usuario.model;

import org.springframework.security.core.GrantedAuthority;

public enum Perfil implements GrantedAuthority {
    ADMINISTRATOR,
    MANAGER,
    OPERATOR;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name(); // padrão Spring Security
    }
}
