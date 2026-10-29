package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import br.com.fiap.oficinamecanicaapi.integracao.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Segurança com a cadeia de filtros real e usuários gravados no banco. Perfis da fase 1 (V10):
 * ADMIN faz tudo; FUNCIONARIO cadastra (sem excluir) e cuida das OS; CLIENTE só vê as próprias OS.
 */
class SegurancaIT extends IntegracaoBase {

    @Test
    @DisplayName("O administrador inicial deve existir após a subida e conseguir logar")
    void administradorInicial() throws Exception {
        Integer admins = jdbcTemplate.queryForObject("""
                select count(*) from usuarios u join perfis p on p.id = u.perfil_id
                where p.nome = 'ADMIN'
                """, Integer.class);

        assertThat(admins).isEqualTo(1);
        assertThat(tokenAdmin()).startsWith("Bearer ");
    }

    @Test
    @DisplayName("Sem token, rotas de negócio e o cadastro de usuários devem responder 401")
    void semToken() throws Exception {
        mockMvc.perform(get("/clientes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/ordens-servico/tempo-medio-execucao")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/minhas-ordens")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson("intruso", "ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token adulterado deve responder 401")
    void tokenAdulterado() throws Exception {
        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, tokenAdmin() + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("FUNCIONARIO deve acessar cadastros e OS")
    void funcionarioComPermissao() throws Exception {
        String funcionario = tokenDe("funcionario", "FUNCIONARIO");

        mockMvc.perform(get("/servico").header(HttpHeaders.AUTHORIZATION, funcionario)).andExpect(status().isOk());
        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, funcionario)).andExpect(status().isOk());
        mockMvc.perform(get("/veiculos").header(HttpHeaders.AUTHORIZATION, funcionario)).andExpect(status().isOk());
        mockMvc.perform(get("/insumos").header(HttpHeaders.AUTHORIZATION, funcionario)).andExpect(status().isOk());
        mockMvc.perform(get("/ordens-servico/tempo-medio-execucao").header(HttpHeaders.AUTHORIZATION, funcionario))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FUNCIONARIO não deve excluir, cadastrar usuários nem ver /minhas-ordens")
    void funcionarioSemPermissao() throws Exception {
        String funcionario = tokenDe("funcionario", "FUNCIONARIO");

        mockMvc.perform(delete("/clientes/1").header(HttpHeaders.AUTHORIZATION, funcionario))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Acesso negado")));
        mockMvc.perform(delete("/servico/1").header(HttpHeaders.AUTHORIZATION, funcionario))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, funcionario)
                        .contentType(MediaType.APPLICATION_JSON).content(cadastroJson("outro", "FUNCIONARIO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/minhas-ordens").header(HttpHeaders.AUTHORIZATION, funcionario))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CLIENTE deve ver só /minhas-ordens; as rotas da oficina respondem 403")
    void clienteSoVeAsPropriasOrdens() throws Exception {
        String cliente = tokenDeCliente("maria", criarCliente());

        mockMvc.perform(get("/minhas-ordens").header(HttpHeaders.AUTHORIZATION, cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, cliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/ordens-servico/tempo-medio-execucao").header(HttpHeaders.AUTHORIZATION, cliente))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cadastro de CLIENTE sem clienteId deve responder 400")
    void clienteSemClienteId() throws Exception {
        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(cadastroJson("semcliente", "CLIENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Informe o clienteId para o perfil CLIENTE")));
    }

    @Test
    @DisplayName("Login com senha errada deve responder 401; cadastro repetido, 409")
    void loginECadastro() throws Exception {
        tokenDe("ana", "FUNCIONARIO");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"ana\",\"senha\":\"Errada@123\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(cadastroJson("ana", "FUNCIONARIO")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("/auth/validate deve aceitar o token emitido pelo login")
    void validarToken() throws Exception {
        mockMvc.perform(get("/auth/validate").header(HttpHeaders.AUTHORIZATION, tokenAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(true)));
    }

    @Test
    @DisplayName("Actuator health deve ser público")
    void healthPublico() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    private long criarCliente() throws Exception {
        String corpo = mockMvc.perform(post("/clientes").header(HttpHeaders.AUTHORIZATION, tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nome":"Maria Silva","documento":"52998224725","email":"maria@email.com",
                                 "telefone":"11987654321"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(corpo);
    }
}
