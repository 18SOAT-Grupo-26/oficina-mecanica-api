package br.com.fiap.oficinamecanicaapi.infrastructure.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sintaxe dos placeholders do application_bkp.yaml. No Spring o padrão é {@code ${VAR:padrão}}; o {@code ${VAR:-padrão}}
 * do shell/Compose vira o valor "-padrão" (porta "-8081" → Tomcat sem porta; e-mail "-" → admin inicial recusado).
 */
class ApplicationYamlTest {

    private static final Pattern PADRAO_DO_SHELL = Pattern.compile("\\$\\{[A-Za-z0-9_.]+:-");

    private static String yaml;

    @BeforeAll
    static void carregar() throws IOException {
        yaml = new ClassPathResource("application_bkp.yaml").getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("Não deve usar a sintaxe ${VAR:-padrão} do shell, que o Spring lê como '-padrão'")
    void naoDeveUsarSintaxeDoShell() {
        assertThat(PADRAO_DO_SHELL.matcher(yaml).results().map(r -> r.group()).toList()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"API_SECRET", "DATASOURCE_PASSWORD"})
    @DisplayName("Segredos obrigatórios não devem ter valor padrão: sem a variável, a API não deve subir")
    void segredosNaoDevemTerPadrao(String variavel) {
        assertThat(yaml).contains("${" + variavel + "}");
    }
}
