package br.com.fiap.oficinamecanicaapi.core.domain.usuario.model;

public record Usuario(
    String nome,
    String email,
    String nomeUsuario,
    String senha,
    boolean trocaSenhaObrigatoria,
    Perfil perfil
) {
}
