package br.com.fiap.oficinamecanicaapi.controller.admin;

import br.com.fiap.oficinamecanicaapi.integracao.IntegracaoBase;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo pela API, com banco real: cadastros → OS → orçamento → execução → entrega.
 * Confere estoque (reserva e baixa), histórico, tempo médio e as métricas de estado.
 */
class FluxoOrdemServicoIT extends IntegracaoBase {

    private static final String CPF = "52998224725";

    @Autowired
    private MeterRegistry meterRegistry;

    private String admin;
    private long clienteId;
    private long veiculoId;
    private long servicoId;
    private long pecaId;

    @BeforeEach
    void cadastros() throws Exception {
        admin = tokenAdmin();

        clienteId = id(criar("/clientes", """
                {"nome":"Maria Silva","documento":"%s","email":"maria@email.com","telefone":"11987654321"}
                """.formatted(CPF)));
        veiculoId = id(criar("/veiculos", """
                {"placa":"BRA2E19","marca":"Fiat","modelo":"Argo","ano":2022,"clienteId":%d}
                """.formatted(clienteId)));
        servicoId = id(criar("/servico", """
                {"nome":"Troca de óleo","descricao":"Troca de óleo do motor","preco":100.00}
                """));
        pecaId = id(criar("/insumos", """
                {"nome":"Filtro de óleo","descricao":"Filtro","quantidade":10,"reservado":0,"preco":25.00}
                """));
    }

    @Test
    @DisplayName("OS percorre todo o ciclo até ENTREGUE e movimenta o estoque")
    void cicloCompleto() throws Exception {
        long osId = id(criar("/ordens-servico", """
                {"documentoCliente":"%s","idVeiculo":%d,
                 "servicos":[{"idServico":%d,"quantidade":1}],
                 "pecas":[{"idPeca":%d,"quantidade":2}]}
                """.formatted(CPF, veiculoId, servicoId, pecaId)));

        executar(get("/ordens-servico/" + osId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("EM_DIAGNOSTICO")));

        executar(patch("/ordens-servico/" + osId + "/gerar-orcamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AGUARDANDO_APROVACAO")));
        assertThat(estoque()).containsExactly(10, 2);

        executar(patch("/ordens-servico/" + osId + "/aprovar-orcamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("EM_EXECUCAO")));

        alterarStatus(osId, "FINALIZADA").andExpect(status().isOk());
        assertThat(estoque()).containsExactly(8, 0);

        alterarStatus(osId, "ENTREGUE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ENTREGUE")));

        executar(get("/ordens-servico/listar/" + clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        executar(get("/ordens-servico/tempo-medio-execucao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeOrdens", is(1)));

        assertThat(meterRegistry.get("oficina.os.quantidade").tag("status", "ENTREGUE").gauge().value())
                .isEqualTo(1.0);
        assertThat(meterRegistry.get("oficina.estoque.pecas.sem.saldo").gauge().value()).isZero();
    }

    @Test
    @DisplayName("Rejeitar o orçamento cancela a OS e devolve a reserva")
    void rejeitarOrcamento() throws Exception {
        long osId = id(criar("/ordens-servico", """
                {"documentoCliente":"%s","idVeiculo":%d,"servicos":[],
                 "pecas":[{"idPeca":%d,"quantidade":3}]}
                """.formatted(CPF, veiculoId, pecaId)));

        executar(patch("/ordens-servico/" + osId + "/gerar-orcamento")).andExpect(status().isOk());
        assertThat(estoque()).containsExactly(10, 3);

        executar(patch("/ordens-servico/" + osId + "/rejeitar-orcamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELADA")));
        assertThat(estoque()).containsExactly(10, 0);
    }

    @Test
    @DisplayName("Abrir OS pedindo mais peças do que o saldo deve responder 409 e não gravar a OS")
    void saldoInsuficiente() throws Exception {
        executar(post("/ordens-servico").contentType(MediaType.APPLICATION_JSON).content("""
                {"documentoCliente":"%s","idVeiculo":%d,"servicos":[],
                 "pecas":[{"idPeca":%d,"quantidade":11}]}
                """.formatted(CPF, veiculoId, pecaId)))
                .andExpect(status().isConflict());

        assertThat(jdbcTemplate.queryForObject("select count(*) from ordem_servico", Integer.class)).isZero();
        assertThat(estoque()).containsExactly(10, 0);
    }

    @Test
    @DisplayName("Transição que pula etapa deve responder 409")
    void transicaoInvalida() throws Exception {
        long osId = id(criar("/ordens-servico", """
                {"documentoCliente":"%s","idVeiculo":%d,"servicos":[{"idServico":%d,"quantidade":1}],"pecas":[]}
                """.formatted(CPF, veiculoId, servicoId)));

        alterarStatus(osId, "ENTREGUE").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cliente e veículo duplicados devem responder 409")
    void duplicados() throws Exception {
        executar(post("/clientes").contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Outra Maria","documento":"%s","email":"outra@email.com","telefone":"11900000000"}
                """.formatted(CPF)))
                .andExpect(status().isConflict());
        executar(post("/veiculos").contentType(MediaType.APPLICATION_JSON).content("""
                {"placa":"BRA2E19","marca":"Fiat","modelo":"Uno","ano":2010,"clienteId":%d}
                """.formatted(clienteId)))
                .andExpect(status().isConflict());
    }

    private String criar(String rota, String json) throws Exception {
        return executar(post(rota).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions alterarStatus(long osId, String destino) throws Exception {
        return executar(patch("/ordens-servico/" + osId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + destino + "\"}"));
    }

    private ResultActions executar(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, admin));
    }

    private java.util.List<Integer> estoque() {
        return jdbcTemplate.queryForObject("select quantidade, reservado from peca_insumo where id = ?",
                (rs, n) -> java.util.List.of(rs.getInt("quantidade"), rs.getInt("reservado")), pecaId);
    }
}
