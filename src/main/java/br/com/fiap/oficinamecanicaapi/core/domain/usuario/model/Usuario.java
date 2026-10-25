package br.com.fiap.oficinamecanicaapi.core.domain.usuario.model;

public record Usuario(
    String nomeUsuario,
    String senha,
    boolean trocaSenhaObrigatoria,
    Perfil perfil
) {
}
