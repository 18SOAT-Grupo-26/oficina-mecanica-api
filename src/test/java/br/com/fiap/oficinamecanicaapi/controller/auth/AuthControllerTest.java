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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cadastro de usuários: só ADMINISTRATOR logado; login continua público. */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String TOKEN = "token-de-teste";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String USERNAME = "admin";
    private static final String CADASTRO_JSON = """
            {"nome":"Bruno Operador","email":"bruno@oficina.com.br","nomeUsuario":"bruno",
             "senha":"Senha@Forte123","trocaSenhaObrigatoria":false,"perfil":"OPERATOR"}
            """;
    private static final CadastroRequest CADASTRO = new CadastroRequest("Bruno Operador", "bruno@oficina.com.br",
            "bruno", "Senha@Forte123", false, Perfil.OPERATOR);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationUseCasePortIn authService;

    @MockitoBean
    private JwtTokenProviderUseCase jwtTokenProvider;

    @MockitoBean
    private UsuarioUseCasePortIn usuarioUseCase;

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
        autenticarComo(Perfil.OPERATOR);

        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(CADASTRO_JSON))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Deve cadastrar quando quem cadastra for ADMINISTRATOR")
    void deveCadastrarComoAdministrador() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);

        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(CADASTRO_JSON))
                .andExpect(status().isCreated());

        verify(authService).cadastrarUsuario(CADASTRO);
    }

    @Test
    @DisplayName("Login deve continuar público, sem token")
    void loginDeveSerPublico() throws Exception {
        SigninRequest login = new SigninRequest(USERNAME, "Senha@Forte123");
        when(authService.autenticar(login)).thenReturn(new AuthToken("Bearer jwt", false));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"admin\",\"senha\":\"Senha@Forte123\"}"))
                .andExpect(status().isOk());
    }

    private void autenticarComo(Perfil perfil) {
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extrairNomeUsuario(TOKEN)).thenReturn(USERNAME);
        when(usuarioUseCase.buscarPorIdentificador(USERNAME))
                .thenReturn(new DetalhesUsuario(new Usuario(null, null, USERNAME, null, false, perfil), Set.of()));
    }
}
