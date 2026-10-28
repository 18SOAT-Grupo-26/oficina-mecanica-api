package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoUseCasePortIn;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServicoController.class)
@Import(SecurityConfig.class)
class ServicoControllerTest {

    private static final String TOKEN = "token-de-teste";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String USERNAME = "usuario";
    private static final Long ID = 1L;
    private static final String ROTA = "/servico";
    private static final ServicoRequest REQUEST = new ServicoRequest("Troca de óleo", "Óleo sintético", new BigDecimal("80.00"));
    private static final ServicoResponse RESPONSE = new ServicoResponse(ID, "Troca de óleo", "Óleo sintético", new BigDecimal("80.00"), null, null);
    private static final String REQUEST_JSON = """
            {"nome":"Troca de óleo","descricao":"Óleo sintético","preco":80.00}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServicoUseCasePortIn useCase;

    @MockitoBean
    private JwtTokenProviderUseCase jwtTokenProvider;

    @MockitoBean
    private UsuarioUseCasePortIn usuarioUseCase;

    @Test
    @DisplayName("Deve criar e responder 201")
    void deveCriar() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.criar(REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(post(ROTA).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.nome").value("Troca de óleo"));
    }

    @Test
    @DisplayName("Deve responder 422 com a mensagem da validação do caso de uso")
    void deveRepassarErroDeValidacao() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.criar(REQUEST)).thenThrow(new ApplicationException(422, "Preço inválido"));

        mockMvc.perform(post(ROTA).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Preço inválido"));
    }

    @Test
    @DisplayName("Deve listar repassando página e tamanho")
    void deveListar() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.listar(2, 5)).thenReturn(new Pagina<>(List.of(RESPONSE), 2, 5, 11));

        mockMvc.perform(get(ROTA).param("page", "2").param("size", "5").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].id").value(ID))
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.total").value(11));
    }

    @Test
    @DisplayName("Deve listar sem paginação informada, repassando nulos ao caso de uso")
    void deveListarSemPaginacao() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.listar(null, null)).thenReturn(new Pagina<>(List.of(), 0, 20, 0));

        mockMvc.perform(get(ROTA).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho").value(20));
    }

    @Test
    @DisplayName("Deve buscar por id")
    void deveBuscarPorId() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.buscarPorId(ID)).thenReturn(RESPONSE);

        mockMvc.perform(get(ROTA + "/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Troca de óleo"));
    }

    @Test
    @DisplayName("Deve responder 404 quando não existir")
    void deveResponderNaoEncontrado() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.buscarPorId(ID)).thenThrow(new ApplicationException(404, "Serviço não encontrado"));

        mockMvc.perform(get(ROTA + "/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Serviço não encontrado"));
    }

    @Test
    @DisplayName("Deve responder 400 quando o id não for numérico, sem chamar o caso de uso")
    void deveRejeitarIdInvalido() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);

        mockMvc.perform(get(ROTA + "/abc").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parâmetro inválido: id"));

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve atualizar")
    void deveAtualizar() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        when(useCase.atualizar(ID, REQUEST)).thenReturn(RESPONSE);

        mockMvc.perform(put(ROTA + "/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    @DisplayName("Deve excluir e responder 204")
    void deveExcluir() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);

        mockMvc.perform(delete(ROTA + "/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isNoContent());

        verify(useCase).excluir(ID);
    }

    @Test
    @DisplayName("Deve responder 403 para MANAGER: serviço é só do administrador")
    void deveNegarGerente() throws Exception {
        autenticarComo(Perfil.MANAGER);

        mockMvc.perform(get(ROTA).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve responder 403 para OPERATOR, sem chamar o caso de uso")
    void deveNegarOperador() throws Exception {
        autenticarComo(Perfil.OPERATOR);

        mockMvc.perform(get(ROTA).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado"));

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve responder 401 sem token, sem chamar o caso de uso")
    void deveExigirToken() throws Exception {
        mockMvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON))
                .andExpect(status().isUnauthorized());

        verify(useCase, never()).criar(any());
    }

    private void autenticarComo(Perfil perfil) {
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extrairNomeUsuario(TOKEN)).thenReturn(USERNAME);
        when(usuarioUseCase.buscarPorIdentificador(USERNAME))
                .thenReturn(new DetalhesUsuario(new Usuario(null, null, USERNAME, null, false, perfil), Set.of()));
    }
}
