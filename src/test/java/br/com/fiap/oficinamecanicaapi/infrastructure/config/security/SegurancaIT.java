package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import br.com.fiap.oficinamecanicaapi.integracao.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Segurança com a cadeia de filtros real e usuários gravados no banco.
 */
class SegurancaIT extends IntegracaoBase {

    @Test
    @DisplayName("O administrador inicial deve existir após a subida e conseguir logar")
    void administradorInicial() throws Exception {
        Integer admins = jdbcTemplate.queryForObject("""
                select count(*) from usuarios u join perfis p on p.id = u.perfil_id
                where p.nome = 'ADMINISTRATOR'
                """, Integer.class);

        assertThat(admins).isEqualTo(1);
        assertThat(tokenAdmin()).startsWith("Bearer ");
    }

    @Test
    @DisplayName("Sem token, rotas de negócio e o cadastro de usuários devem responder 401")
    void semToken() throws Exception {
        mockMvc.perform(get("/clientes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/ordens-servico/tempo-medio-execucao")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson("intruso", "ADMINISTRATOR")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token adulterado deve responder 401")
    void tokenAdulterado() throws Exception {
        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, tokenAdmin() + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Perfil sem permissão deve responder 403 Acesso negado, inclusive no cadastro de usuários")
    void perfilSemPermissao() throws Exception {
        String gerente = tokenDe("gerente", "MANAGER");
        String operador = tokenDe("operador", "OPERATOR");

        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, gerente))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("Acesso negado")));
        mockMvc.perform(get("/insumos").header(HttpHeaders.AUTHORIZATION, operador))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, gerente)
                        .contentType(MediaType.APPLICATION_JSON).content(cadastroJson("outro", "OPERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Perfil com permissão deve passar")
    void perfilComPermissao() throws Exception {
        String gerente = tokenDe("gerente", "MANAGER");

        mockMvc.perform(get("/insumos").header(HttpHeaders.AUTHORIZATION, gerente)).andExpect(status().isOk());
        mockMvc.perform(get("/clientes").header(HttpHeaders.AUTHORIZATION, tokenAdmin())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login com senha errada deve responder 401; cadastro repetido, 409")
    void loginECadastro() throws Exception {
        tokenDe("ana", "OPERATOR");

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"ana\",\"senha\":\"Errada@123\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/auth/cadastro").header(HttpHeaders.AUTHORIZATION, tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(cadastroJson("ana", "OPERATOR")))
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
}
