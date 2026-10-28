package br.com.fiap.oficinamecanicaapi.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.DockerClientFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração (*IT): contexto completo + PostgreSQL em container + banco limpo a cada teste.
 * Não é um teste: os *IT de cada pacote estendem esta classe.
 *
 * <p>O AdministradorInicial cria o ADMINISTRATOR "admin" na subida do contexto, com os api.admin.* abaixo;
 * a limpeza preserva esse usuário. Outros usuários são cadastrados com o token dele, como na aplicação.
 * Sem Docker na máquina, os testes são pulados (não falham).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
@EnabledIf("br.com.fiap.oficinamecanicaapi.integracao.IntegracaoBase#dockerDisponivel")
@TestPropertySource(properties = {
        // só para teste: 64 bytes em base64, exigência do HS512
        "api.secret=dGVzdGUtaW50ZWdyYWNhby1vZmljaW5hLW1lY2FuaWNhLWFwaS1zZWdyZWRvLWNvbS02NC1ieXRlcy1vay0x",
        "api.admin.nome=Administrador",
        "api.admin.email=" + IntegracaoBase.ADMIN + "@oficina.com.br",
        "api.admin.usuario=" + IntegracaoBase.ADMIN,
        "api.admin.senha=" + IntegracaoBase.SENHA,
        // url, usuário e senha reais vêm do @ServiceConnection
        "spring.datasource.password=ignorado"
})
public abstract class IntegracaoBase {

    protected static final String ADMIN = "admin";
    protected static final String SENHA = "Senha@Forte2026";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    public static boolean dockerDisponivel() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception _) {
            return false;
        }
    }

    @BeforeEach
    protected void limparBanco() {
        // perfis e permissões vêm das migrations; o administrador inicial vem da subida do contexto
        jdbcTemplate.execute("""
                truncate table historico_status_os, item_peca_os, item_servico_os, ordem_servico,
                               veiculos, clientes, servico, peca_insumo
                restart identity cascade
                """);
        jdbcTemplate.update("delete from usuarios where nome_usuario <> ?", ADMIN);
    }

    // ---- autenticação pela API (token já com o prefixo "Bearer ") ---------------------------------

    protected String tokenAdmin() throws Exception {
        return login(ADMIN);
    }

    /** Cadastra o usuário com o token do administrador e devolve o token dele. */
    protected String tokenDe(String nomeUsuario, String perfil) throws Exception {
        mockMvc.perform(post("/auth/cadastro")
                        .header(HttpHeaders.AUTHORIZATION, tokenAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cadastroJson(nomeUsuario, perfil)))
                .andExpect(status().isCreated());
        return login(nomeUsuario);
    }

    protected String login(String nomeUsuario) throws Exception {
        String corpo = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identificador":"%s","senha":"%s"}
                                """.formatted(nomeUsuario, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    protected static String cadastroJson(String nomeUsuario, String perfil) {
        return """
                {"nome":"Usuário %1$s","email":"%1$s@oficina.com.br","nomeUsuario":"%1$s",
                 "senha":"%2$s","trocaSenhaObrigatoria":false,"perfil":"%3$s"}
                """.formatted(nomeUsuario, SENHA, perfil);
    }

    protected static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
