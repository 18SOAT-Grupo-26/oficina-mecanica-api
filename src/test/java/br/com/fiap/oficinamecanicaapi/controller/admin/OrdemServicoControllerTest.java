package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.TempoMedioExecucaoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.auth.JwtTokenProviderUseCase;
import br.com.fiap.oficinamecanicaapi.infrastructure.config.security.SecurityConfig;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdemServicoController.class)
@Import(SecurityConfig.class)
class OrdemServicoControllerTest {

    private static final String TOKEN = "token-de-teste";
    private static final String BEARER = "Bearer " + TOKEN;
    private static final String USERNAME = "admin";
    private static final Long ID = 99L;
    private static final OffsetDateTime ABERTURA = OffsetDateTime.parse("2026-10-24T08:00:00-03:00");
    private static final Long ID_CLIENTE = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrdemServicoUseCasePortIn useCase;

    @MockitoBean
    private JwtTokenProviderUseCase jwtTokenProvider;

    @MockitoBean
    private UsuarioUseCasePortIn usuarioUseCase;

    @Test
    @DisplayName("Deve criar a OS com cliente e veículo e responder 201")
    void deveCriarOs() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.criar(new OrdemServicoRequest("52998224725", 20L, List.of(), List.of()))).thenReturn(response(EM_DIAGNOSTICO));

        mockMvc.perform(post("/ordens-servico").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"documentoCliente":"52998224725","idVeiculo":20,"servicos":[],"pecas":[]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"))
                .andExpect(jsonPath("$.idCliente").value(10))
                .andExpect(jsonPath("$.historico[0].statusAntigo").isEmpty())
                .andExpect(jsonPath("$.historico[0].statusAtualizado").value("RECEBIDA"));
    }

    @Test
    @DisplayName("Deve responder 422 quando não há body, sem chamar o caso de uso")
    void deveRecusarCriarSemBody() throws Exception {
        autenticarComo(Perfil.ADMIN);

        mockMvc.perform(post("/ordens-servico").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isUnprocessableEntity());

        verify(useCase, never()).criar(any());
    }

    @Test
    @DisplayName("Deve responder 400 quando falta cliente ou veículo, sem chamar o caso de uso")
    void deveRecusarCriarSemCamposObrigatorios() throws Exception {
        autenticarComo(Perfil.ADMIN);

        mockMvc.perform(post("/ordens-servico").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"documentoCliente":"52998224725","servicos":[],"pecas":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campos inválidos: idVeiculo"));
        mockMvc.perform(post("/ordens-servico").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"documentoCliente":" ","idVeiculo":20,"servicos":[],"pecas":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campos inválidos: documentoCliente"));

        verify(useCase, never()).criar(any());
    }

    @Test
    @DisplayName("Deve consultar a OS com status e histórico")
    void deveConsultarOs() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.consultar(ID)).thenReturn(response(AGUARDANDO_APROVACAO));

        mockMvc.perform(get("/ordens-servico/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.itensPeca[0].subtotal").value(60.00))
                .andExpect(jsonPath("$.historico[0].dataHora").exists());
    }

    @Test
    @DisplayName("Deve responder 404 quando a OS não existe")
    void deveResponderOsNaoEncontrada() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.consultar(ID)).thenThrow(new ApplicationException(404, "Ordem de serviço não encontrada"));

        mockMvc.perform(get("/ordens-servico/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ordem de serviço não encontrada"));
    }

    @Test
    @DisplayName("Deve responder 400 quando faltam as listas de itens ou a quantidade é zero, sem chamar o caso de uso")
    void deveRecusarItensInvalidos() throws Exception {
        autenticarComo(Perfil.ADMIN);

        mockMvc.perform(post("/ordens-servico").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"documentoCliente":"52998224725","idVeiculo":20}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campos inválidos: pecas, servicos"));
        mockMvc.perform(post("/ordens-servico").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"documentoCliente":"52998224725","idVeiculo":20,"servicos":[],
                                 "pecas":[{"idPeca":3,"quantidade":0}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campos inválidos: pecas[0].quantidade"));

        verify(useCase, never()).criar(any());
    }

    @Test
    @DisplayName("Deve atualizar a OS com o PUT completo repassando-o ao caso de uso")
    void deveAtualizarOs() throws Exception {
        autenticarComo(Perfil.ADMIN);
        OrdemServicoRequest request = new OrdemServicoRequest("52998224725", 20L,
                List.of(new OrdemServicoRequest.Servico(7L, 1)), List.of());
        when(useCase.atualizar(ID, request)).thenReturn(response(EM_DIAGNOSTICO));

        mockMvc.perform(put("/ordens-servico/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"documentoCliente":"52998224725","idVeiculo":20,
                                 "servicos":[{"idServico":7,"quantidade":1}],"pecas":[]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
    }

    @Test
    @DisplayName("Deve responder 400 no PUT sem serviços ou peças, sem chamar o caso de uso")
    void deveRecusarPutSemItens() throws Exception {
        autenticarComo(Perfil.ADMIN);

        mockMvc.perform(put("/ordens-servico/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"documentoCliente":"52998224725","idVeiculo":20,"servicos":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campos inválidos: pecas"));

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve mudar o status pelo PATCH repassando o status de destino ao caso de uso")
    void deveAlterarStatus() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.alterarStatus(ID, new OrdemServicoRequest.Status(FINALIZADA))).thenReturn(response(FINALIZADA));

        mockMvc.perform(patch("/ordens-servico/{id}/status", ID).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"FINALIZADA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"));
    }

    @Test
    @DisplayName("Deve gerar o orçamento pelo PATCH")
    void deveGerarOrcamento() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.registrarOrcamentoEnviado(ID)).thenReturn(response(AGUARDANDO_APROVACAO));

        mockMvc.perform(patch("/ordens-servico/{id}/gerar-orcamento", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"));
    }

    @Test
    @DisplayName("Deve aprovar o orçamento pelo PATCH")
    void deveAprovarOrcamento() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.registrarOrcamentoAprovado(ID)).thenReturn(response(EM_EXECUCAO));

        mockMvc.perform(patch("/ordens-servico/{id}/aprovar-orcamento", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    @DisplayName("Deve rejeitar o orçamento pelo PATCH")
    void deveRejeitarOrcamento() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.registrarOrcamentoRejeitado(ID)).thenReturn(response(CANCELADA));

        mockMvc.perform(patch("/ordens-servico/{id}/rejeitar-orcamento", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    @DisplayName("Deve cancelar a OS pelo DELETE e responder 204")
    void deveCancelarOs() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);

        mockMvc.perform(delete("/ordens-servico/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isNoContent());

        verify(useCase).cancelar(ID);
    }

    @Test
    @DisplayName("Deve responder 409 no DELETE quando a OS não pode mais ser cancelada")
    void deveResponderConflitoAoCancelar() throws Exception {
        autenticarComo(Perfil.ADMINISTRATOR);
        doThrow(new ApplicationException(409, "Transição inválida: EM_EXECUCAO → CANCELADA"))
                .when(useCase).cancelar(ID);

        mockMvc.perform(delete("/ordens-servico/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Transição inválida: EM_EXECUCAO → CANCELADA"));
    }

    @Test
    @DisplayName("Deve responder 409 com a mensagem da transição inválida")
    void deveResponderTransicaoInvalida() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.alterarStatus(ID, new OrdemServicoRequest.Status(FINALIZADA)))
                .thenThrow(new ApplicationException(409, "Transição inválida: AGUARDANDO_APROVACAO → FINALIZADA"));

        mockMvc.perform(patch("/ordens-servico/{id}/status", ID).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"FINALIZADA"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Transição inválida: AGUARDANDO_APROVACAO → FINALIZADA"));
    }

    @Test
    @DisplayName("Deve responder 400 quando o PATCH não traz o status de destino, sem chamar o caso de uso")
    void deveExigirStatusNoPatch() throws Exception {
        autenticarComo(Perfil.ADMIN);

        mockMvc.perform(patch("/ordens-servico/{id}/status", ID).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Campos inválidos: status"));

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve responder 422 para status desconhecido sem chamar o caso de uso")
    void deveRejeitarStatusDesconhecido() throws Exception {
        autenticarComo(Perfil.ADMIN);

        mockMvc.perform(patch("/ordens-servico/{id}/status", ID).header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"status":"PRONTA"}
                                """))
                .andExpect(status().isUnprocessableContent());

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve consultar o tempo médio de execução")
    void deveConsultarTempoMedio() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.consultarTempoMedioExecucao()).thenReturn(new TempoMedioExecucaoResponse(120L, 3));

        mockMvc.perform(get("/ordens-servico/tempo-medio-execucao").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaMinutos").value(120))
                .andExpect(jsonPath("$.quantidadeOrdens").value(3));
    }

    @Test
    @DisplayName("Deve responder 200 com média nula quando nenhuma OS foi finalizada")
    void deveConsultarTempoMedioSemOs() throws Exception {
        autenticarComo(Perfil.ADMIN);
        when(useCase.consultarTempoMedioExecucao()).thenReturn(new TempoMedioExecucaoResponse(null, 0));

        mockMvc.perform(get("/ordens-servico/tempo-medio-execucao").header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaMinutos").isEmpty())
                .andExpect(jsonPath("$.quantidadeOrdens").value(0));
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"ADMIN", "FUNCIONARIO"})
    @DisplayName("Deve aceitar o admin e o funcionário")
    void deveAceitarAdminEFuncionario(Perfil perfil) throws Exception {
        autenticarComo(perfil);
        when(useCase.criar(any())).thenReturn(response(EM_DIAGNOSTICO));

        mockMvc.perform(post("/ordens-servico").header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"documentoCliente":"52998224725","idVeiculo":20,"servicos":[],"pecas":[]}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Deve responder 401 quando a requisição não tiver token")
    void deveResponderNaoAutenticado() throws Exception {
        mockMvc.perform(post("/ordens-servico"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("Deve responder 403 quando o cliente tentar usar as rotas dos funcionários")
    void deveNegarAcessoAoCliente() throws Exception {
        autenticarComo(Perfil.CLIENTE);

        mockMvc.perform(get("/ordens-servico/{id}", ID).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isForbidden());

        verifyNoInteractions(useCase);
    }

    private static OrdemServicoResponse response(StatusOrdemServico status) {
        return new OrdemServicoResponse(ID, 10L, 20L, status,
                List.of(new OrdemServicoResponse.ItemServico(1L, 7L, "Troca de óleo",
                        new BigDecimal("100.00"), 1, new BigDecimal("100.00"))),
                List.of(new OrdemServicoResponse.ItemPeca(2L, 3L, "Filtro de óleo",
                        new BigDecimal("30.00"), 2, new BigDecimal("60.00"))),
                List.of(new OrdemServicoResponse.RegistroHistorico(null, RECEBIDA, ABERTURA)));
    }

    private void autenticarComo(Perfil perfil) {
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extrairNomeUsuario(TOKEN)).thenReturn(USERNAME);
        when(usuarioUseCase.buscarPorIdentificador(USERNAME))
                .thenReturn(new DetalhesUsuario(new Usuario(null, null, USERNAME, null, false, perfil), Set.of()));
    }

    @Test
    @DisplayName("Deve listar as OS do cliente")
    void deveListarOrdensDoCliente() throws Exception {
        autenticarComo(Perfil.FUNCIONARIO);
        when(useCase.listar(ID_CLIENTE)).thenReturn(List.of(
                new OrdemServicoResponse(2L, ID_CLIENTE, 20L, StatusOrdemServico.EM_DIAGNOSTICO, List.of(), List.of(), List.of()),
                new OrdemServicoResponse(1L, ID_CLIENTE, 20L, StatusOrdemServico.ENTREGUE, List.of(), List.of(), List.of())));

        mockMvc.perform(get("/ordens-servico/listar/{idCliente}", ID_CLIENTE).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[1].status").value("ENTREGUE"));
    }

    @Test
    @DisplayName("Deve responder lista vazia quando o cliente não tem OS")
    void deveResponderListaVazia() throws Exception {
        autenticarComo(Perfil.FUNCIONARIO);
        when(useCase.listar(ID_CLIENTE)).thenReturn(List.of());

        mockMvc.perform(get("/ordens-servico/listar/{idCliente}", ID_CLIENTE).header(HttpHeaders.AUTHORIZATION, BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Deve responder 401 sem token")
    void deveExigirToken() throws Exception {
        mockMvc.perform(get("/ordens-servico/listar/{idCliente}", ID_CLIENTE))
                .andExpect(status().isUnauthorized());
    }
}
