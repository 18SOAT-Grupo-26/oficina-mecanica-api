package br.com.fiap.oficinamecanicaapi.core.domain.usuario.model;

/**
 * @param clienteId cadastro do cliente ligado ao usuário; só o perfil CLIENTE tem (as OS que ele pode ver)
 */
public record Usuario(
        String nome,
        String email,
        String nomeUsuario,
        String senha,
        boolean trocaSenhaObrigatoria,
        Perfil perfil,
        Long clienteId
) {

    public Usuario(String nome, String email, String nomeUsuario, String senha,
                   boolean trocaSenhaObrigatoria, Perfil perfil) {
        this(nome, email, nomeUsuario, senha, trocaSenhaObrigatoria, perfil, null);
    }
}
