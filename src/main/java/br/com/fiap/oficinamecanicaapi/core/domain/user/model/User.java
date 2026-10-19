package br.com.fiap.oficinamecanicaapi.core.domain.user.model;

public record User(
    String username,
    String password,
    boolean changeRequired,
    Role role
) {
}
