package br.com.fiap.oficinamecanicaapi.core.port.in.auth;


public interface AuthenticationUseCasePortIn {

    void cadastrarUsuario(CadastroRequest request);

    AuthToken autenticar(SigninRequest request);

    Boolean validarToken(String token);

}
