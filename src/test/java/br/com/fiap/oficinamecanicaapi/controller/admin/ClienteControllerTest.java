package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.TipoDocumento;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.cliente.ClienteUseCasePortIn;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
class ClienteControllerTest {

    private static final String TOKEN = "token-de-teste";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String USERNAME = "admin";
    private static final Long ID = 1L;
    private static final String NOME = "Maria da Silva";
    private static final String CPF = "52998224725";
    private static final String EMAIL = "maria@email.com";
    private static final String TELEFONE = "11999998888";
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-21T10:00:00-03:00");
    private static final ClienteRequest REQUEST = new ClienteRequest(NOME, CPF, EMAIL, TELEFONE, null);
    private static final ClienteResponse RESPONSE = new ClienteResponse(ID, NOME, CPF, TipoDocumento.CPF, EMAIL,
            TELEFONE, null, CRIADO_EM, CRIADO_EM);
    private static final String REQUEST_JSON = """
            {"nome":"%s","documento":"%s","email":"%s","telefone":"%s"}
            """.formatted(NOME, CPF, EMAIL, TELEFONE);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteUseCasePortIn useCase;

    @MockitoBean
    private JwtTokenProviderUseCase jwtTokenProvider;

    @MockitoBean
    private UsuarioUseCasePortIn usuarioUseCase;

    @Test
    @DisplayName("Deve cadastrar cliente e responder 201 para administrador")
    void deveCadastrarCliente() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.criar(REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(post("/clientes").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.tipoDocumento").value("CPF"));
    }

    @Test
    @DisplayName("Deve responder 400 listando os campos inválidos sem chamar o caso de uso")
    void deveRejeitarCorpoInvalido() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        String corpoInvalido = """
                {"nome":"","documento":"%s","email":"sem-arroba","telefone":"%s"}
                """.formatted(CPF, TELEFONE);

        mockMvc.perform(post("/clientes").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(400))
                .andExpect(jsonPath("$.message").value("Campos inválidos: email, nome"));

        verify(useCase, never()).criar(any(ClienteRequest.class));
    }

    @Test
    @DisplayName("Deve responder 409 quando o documento já estiver cadastrado")
    void deveResponderConflito() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.criar(REQUEST)).thenThrow(
                new ApplicationException(409, "Já existe um cliente cadastrado com este documento"));

        mockMvc.perform(post("/clientes").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value(409));
    }

    @Test
    @DisplayName("Deve listar clientes repassando página e tamanho")
    void deveListarClientes() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.listar(1, 5)).thenReturn(new Pagina<>(List.of(RESPONSE), 1, 5, 6));

        mockMvc.perform(get("/clientes").param("page", "1").param("size", "5")
                        .header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].id").value(ID))
                .andExpect(jsonPath("$.total").value(6));
    }

    @Test
    @DisplayName("Deve buscar cliente por id")
    void deveBuscarPorId() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.buscarPorId(ID)).thenReturn(RESPONSE);

        mockMvc.perform(get("/clientes/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(NOME));
    }

    @Test
    @DisplayName("Deve responder 404 quando o cliente não existir")
    void deveResponderNaoEncontrado() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.buscarPorId(ID)).thenThrow(new ApplicationException(404, "Cliente não encontrado"));

        mockMvc.perform(get("/clientes/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente não encontrado"));
    }

    @Test
    @DisplayName("Deve responder 400 quando o id não for numérico")
    void deveRejeitarIdNaoNumerico() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);

        mockMvc.perform(get("/clientes/abc").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro inválido: id"));
    }

    @Test
    @DisplayName("Deve buscar cliente por documento")
    void deveBuscarPorDocumento() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.buscarPorDocumento(CPF)).thenReturn(RESPONSE);

        mockMvc.perform(get("/clientes/documento/{documento}", CPF).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documento").value(CPF));
    }

    @Test
    @DisplayName("Deve atualizar cliente e responder 200")
    void deveAtualizarCliente() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.atualizar(ID, REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(put("/clientes/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    @DisplayName("Deve desativar cliente e responder 204")
    void deveDesativarCliente() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);

        mockMvc.perform(delete("/clientes/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isNoContent());

        verify(useCase).desativar(ID);
    }

    @Test
    @DisplayName("Deve responder 401 quando a requisição não tiver token")
    void deveResponderNaoAutenticado() throws Exception {
        mockMvc.perform(get("/clientes"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve responder 403 quando o usuário não tiver o perfil ADMINISTRATOR")
    void deveResponderAcessoNegado() throws Exception {
        autenticarComo(Perfil.OPERATOR);

        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(403));

        verifyNoInteractions(useCase);
    }

    private void autenticarComo(Perfil perfil) {
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extrairNomeUsuario(TOKEN)).thenReturn(USERNAME);
        when(usuarioUseCase.buscarPorIdentificador(USERNAME))
                .thenReturn(new DetalhesUsuario(new Usuario(null, null, USERNAME, null, false, perfil), Set.of()));
    }
}
