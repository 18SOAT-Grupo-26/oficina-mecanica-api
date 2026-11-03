package br.com.fiap.oficinamecanicaapi.core.domain.usuario.model;

import org.springframework.security.core.GrantedAuthority;

public enum Perfil implements GrantedAuthority {
    ADMIN,
    FUNCIONARIO,
    CLIENTE;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name(); // padrão Spring Security
    }
}
