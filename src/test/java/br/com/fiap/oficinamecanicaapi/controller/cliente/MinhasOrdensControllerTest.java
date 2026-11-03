package br.com.fiap.oficinamecanicaapi.controller.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.auth.JwtTokenUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.infrastructure.config.security.SecurityConfig;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.EM_EXECUCAO;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MinhasOrdensController.class)
@Import(SecurityConfig.class)
class MinhasOrdensControllerTest {

    private static final String TOKEN = "token-de-teste";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String USERNAME = "maria";
    private static final Long ID = 99L;
    private static final Long ID_CLIENTE = 10L;
    private static final OrdemServicoResponse RESPONSE =
            new OrdemServicoResponse(ID, ID_CLIENTE, 20L, EM_EXECUCAO, List.of(), List.of(), List.of());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdemServicoUseCasePortIn useCase;

    @MockitoBean
    private JwtTokenUseCasePortIn jwtTokenProvider;

    @MockitoBean
    private UsuarioUseCasePortIn usuarioUseCase;

    @Test
    @DisplayName("Deve listar as OS do cliente do token")
    void deveListarOsDoCliente() throws Exception {
        autenticarComo(Perfil.CLIENTE, ID_CLIENTE);
        when(useCase.listarDoCliente(ID_CLIENTE)).thenReturn(List.of(RESPONSE));

        mockMvc.perform(get("/minhas-ordens").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ID))
                .andExpect(jsonPath("$[0].status").value("EM_EXECUCAO"));
    }

    @Test
    @DisplayName("Deve consultar a OS repassando o cliente do token")
    void deveConsultarOsDoCliente() throws Exception {
        autenticarComo(Perfil.CLIENTE, ID_CLIENTE);
        when(useCase.consultarDoCliente(ID, ID_CLIENTE)).thenReturn(RESPONSE);

        mockMvc.perform(get("/minhas-ordens/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    @DisplayName("Deve responder 404 para a OS de outro cliente")
    void deveResponder404ParaOsDeOutroCliente() throws Exception {
        autenticarComo(Perfil.CLIENTE, ID_CLIENTE);
        when(useCase.consultarDoCliente(ID, ID_CLIENTE))
                .thenThrow(new RecursoNaoEncontradoException("Ordem de serviço não encontrada"));

        mockMvc.perform(get("/minhas-ordens/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve responder 403 para perfis diferentes de CLIENTE")
    void deveNegarAcessoAosFuncionarios() throws Exception {
        autenticarComo(Perfil.FUNCIONARIO, null);

        mockMvc.perform(get("/minhas-ordens").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve responder 401 sem token")
    void deveResponderNaoAutenticado() throws Exception {
        mockMvc.perform(get("/minhas-ordens"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    private void autenticarComo(Perfil perfil, Long clienteId) {
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extrairNomeUsuario(TOKEN)).thenReturn(USERNAME);
        when(usuarioUseCase.buscarPorIdentificador(USERNAME)).thenReturn(new DetalhesUsuario(
                new Usuario(null, null, USERNAME, null, false, perfil, clienteId), Set.of()));
    }
}
