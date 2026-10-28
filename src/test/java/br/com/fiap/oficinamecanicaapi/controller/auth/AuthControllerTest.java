package br.com.fiap.oficinamecanicaapi.controller.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.CadastroRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.AuthenticationUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.auth.JwtTokenProviderUseCase;
import br.com.fiap.oficinamecanicaapi.infrastructure.config.security.SecurityConfig;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cadastro de usuários: só ADMINISTRATOR logado; login e validação do token continuam públicos. */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String TOKEN = "token-de-teste";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String USERNAME = "admin";
    private static final String CADASTRO_JSON = """
            {"nome":"Bruno Operador","email":"bruno@oficina.com.br","nomeUsuario":"bruno",
             "senha":"Senha@Forte123","trocaSenhaObrigatoria":false,"perfil":"FUNCIONARIO"}
            """;
    private static final CadastroRequest CADASTRO = new CadastroRequest("Bruno Operador", "bruno@oficina.com.br",
            "bruno", "Senha@Forte123", false, Perfil.FUNCIONARIO, null);
    private static final String LOGIN_JSON = """
            {"identificador":"admin","senha":"Senha@Forte123"}
            """;
    private static final SigninRequest LOGIN = new SigninRequest(USERNAME, "Senha@Forte123");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationUseCasePortIn authService;

    @MockitoBean
    private JwtTokenProviderUseCase jwtTokenProvider;

    @MockitoBean
    private UsuarioUseCasePortIn usuarioUseCase;

    // ---- cadastro (somente ADMINISTRATOR) ---------------------------------------------------------

    @Test
    @DisplayName("Deve responder 401 ao cadastrar usuário sem token")
    void deveExigirTokenNoCadastro() throws Exception {
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(CADASTRO_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Deve responder 403 quando quem cadastra não for ADMINISTRATOR")
    void deveRecusarCadastroDeQuemNaoEhAdministrador() throws Exception {
        autenticarComo(Perfil.FUNCIONARIO);

        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(CADASTRO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Deve cadastrar quando quem cadastra for ADMINISTRATOR")
    void deveCadastrarComoAdministrador() throws Exception {
        autenticarComo(Perfil.ADMIN);

        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(CADASTRO_JSON))
                .andExpect(status().isCreated());

        verify(authService).cadastrarUsuario(CADASTRO);
    }

    @Test
    @DisplayName("Deve responder 400 listando os campos obrigatórios ausentes no cadastro")
    void deveRejeitarCadastroIncompleto() throws Exception {
        autenticarComo(Perfil.ADMIN);

        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                            {"nome":"","email":"bruno@oficina.com.br","nomeUsuario":"bruno","senha":"Senha@Forte123",
                             "trocaSenhaObrigatoria":false}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campos inválidos: nome, perfil"));

        verify(authService, never()).cadastrarUsuario(any());
    }

    @Test
    @DisplayName("Deve responder 409 quando o e-mail ou o nome de usuário já existir")
    void deveResponderConflitoNoCadastro() throws Exception {
        autenticarComo(Perfil.ADMIN);
        doThrow(new ApplicationException(409, "Já existe um usuário cadastrado com este e-mail"))
                .when(authService).cadastrarUsuario(CADASTRO);

        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(CADASTRO_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value(409));
    }

    // ---- login e validação (públicos) -------------------------------------------------------------

    @Test
    @DisplayName("Login deve continuar público, sem token, e devolver o token gerado")
    void loginDeveSerPublico() throws Exception {
        when(authService.autenticar(LOGIN)).thenReturn(new AuthToken("Bearer jwt", false));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("Bearer jwt"))
                .andExpect(jsonPath("$.trocaSenhaObrigatoria").value(false));
    }

    @Test
    @DisplayName("Deve responder 401 com a mensagem genérica quando as credenciais não conferem")
    void deveRecusarCredenciais() throws Exception {
        when(authService.autenticar(LOGIN)).thenThrow(new ApplicationException(401, "Credenciais inválidas"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("Deve responder 400 no login sem senha, sem chamar o caso de uso")
    void deveRejeitarLoginIncompleto() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"admin\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campos inválidos: senha"));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Deve validar o token recebido no header Authorization")
    void deveValidarToken() throws Exception {
        when(authService.validarToken("Bearer jwt")).thenReturn(true);

        mockMvc.perform(get("/auth/validate").header(HttpHeaders.AUTHORIZATION, "Bearer jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @DisplayName("Deve responder 400 quando o header Authorization não vier no validate")
    void deveExigirHeaderNoValidate() throws Exception {
        mockMvc.perform(get("/auth/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Header obrigatório ausente: Authorization"));

        verifyNoInteractions(authService);
    }

    private void autenticarComo(Perfil perfil) {
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extrairNomeUsuario(TOKEN)).thenReturn(USERNAME);
        when(usuarioUseCase.buscarPorIdentificador(USERNAME))
                .thenReturn(new DetalhesUsuario(new Usuario(null, null, USERNAME, null, false, perfil), Set.of()));
    }
}
