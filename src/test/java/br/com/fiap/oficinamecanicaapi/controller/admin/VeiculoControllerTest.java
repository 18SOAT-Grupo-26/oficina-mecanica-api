package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.JwtTokenUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoUseCasePortIn;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VeiculoController.class)
@Import(SecurityConfig.class)
class VeiculoControllerTest {

    private static final String TOKEN = "token-de-teste";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String USERNAME = "admin";
    private static final Long ID = 5L;
    private static final Long CLIENTE_ID = 1L;
    private static final String PLACA = "BRA2E19";
    private static final String MARCA = "Volkswagen";
    private static final String MODELO = "Gol";
    private static final Integer ANO = 2020;
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-22T10:00:00-03:00");
    private static final VeiculoRequest REQUEST = new VeiculoRequest(PLACA, MARCA, MODELO, ANO, CLIENTE_ID);
    private static final VeiculoResponse RESPONSE = new VeiculoResponse(ID, PLACA, MARCA, MODELO, ANO, CLIENTE_ID,
            CRIADO_EM, CRIADO_EM);
    private static final String REQUEST_JSON = """
            {"placa":"%s","marca":"%s","modelo":"%s","ano":%d,"clienteId":%d}
            """.formatted(PLACA, MARCA, MODELO, ANO, CLIENTE_ID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VeiculoUseCasePortIn useCase;

    @MockitoBean
    private JwtTokenUseCasePortIn jwtTokenProvider;

    @MockitoBean
    private UsuarioUseCasePortIn usuarioUseCase;

    @Test
    @DisplayName("Deve cadastrar veículo e responder 201 para administrador")
    void deveCadastrarVeiculo() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.criar(REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(post("/veiculos").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.placa").value(PLACA));
    }

    @Test
    @DisplayName("Deve responder 400 listando os campos inválidos sem chamar o caso de uso")
    void deveRejeitarCorpoInvalido() throws Exception {
        autenticarComo(Perfil.ADMIN);
        String corpoInvalido = """
                {"placa":"","marca":"%s","modelo":"%s"}
                """.formatted(MARCA, MODELO);

        mockMvc.perform(post("/veiculos").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(corpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campos inválidos: ano, clienteId, placa"));

        verify(useCase, never()).criar(any(VeiculoRequest.class));
    }

    @Test
    @DisplayName("Deve responder 404 ao cadastrar veículo para cliente inexistente")
    void deveResponderClienteNaoEncontrado() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.criar(REQUEST)).thenThrow(new ApplicationException(404, "Cliente não encontrado"));

        mockMvc.perform(post("/veiculos").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Cliente não encontrado"));
    }

    @Test
    @DisplayName("Deve responder 409 quando a placa já estiver cadastrada")
    void deveResponderConflito() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.criar(REQUEST)).thenThrow(
                new ApplicationException(409, "Já existe um veículo cadastrado com esta placa"));

        mockMvc.perform(post("/veiculos").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deve listar veículos filtrando por cliente e repassando a paginação")
    void deveListarVeiculosDoCliente() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.listar(CLIENTE_ID, 0, 10)).thenReturn(new Pagina<>(List.of(RESPONSE), 0, 10, 1));

        mockMvc.perform(get("/veiculos").param("clienteId", "1").param("page", "0").param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].clienteId").value(CLIENTE_ID))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("Deve buscar veículo por id")
    void deveBuscarPorId() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.buscarPorId(ID)).thenReturn(RESPONSE);

        mockMvc.perform(get("/veiculos/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value(MODELO));
    }

    @Test
    @DisplayName("Deve buscar veículo por placa")
    void deveBuscarPorPlaca() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.buscarPorPlaca(PLACA)).thenReturn(RESPONSE);

        mockMvc.perform(get("/veiculos/placa/{placa}", PLACA).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value(PLACA));
    }

    @Test
    @DisplayName("Deve atualizar veículo e responder 200")
    void deveAtualizarVeiculo() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.atualizar(ID, REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(put("/veiculos/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    @DisplayName("Deve desativar veículo e responder 204")
    void deveDesativarVeiculo() throws Exception {
        autenticarComo(Perfil.ADMIN);

        mockMvc.perform(delete("/veiculos/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isNoContent());

        verify(useCase).desativar(ID);
    }

    @Test
    @DisplayName("Deve responder 401 quando a requisição não tiver token")
    void deveResponderNaoAutenticado() throws Exception {
        mockMvc.perform(get("/veiculos"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve responder 403 quando o funcionário tentar desativar veículo")
    void deveResponderAcessoNegado() throws Exception {
        autenticarComo(Perfil.FUNCIONARIO);

        mockMvc.perform(delete("/veiculos/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve responder 403 quando o cliente tentar acessar o cadastro de veículos")
    void deveNegarAcessoAoCliente() throws Exception {
        autenticarComo(Perfil.CLIENTE);

        mockMvc.perform(get("/veiculos").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    private void autenticarComo(Perfil perfil) {
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extrairNomeUsuario(TOKEN)).thenReturn(USERNAME);
        when(usuarioUseCase.buscarPorIdentificador(USERNAME))
                .thenReturn(new DetalhesUsuario(new Usuario(null, null, USERNAME, null, false, perfil), Set.of()));
    }
}
