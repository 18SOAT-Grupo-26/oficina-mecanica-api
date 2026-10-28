package br.com.fiap.oficinamecanicaapi.infrastructure.persistence;

import br.com.fiap.oficinamecanicaapi.integracao.IntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MigracoesIT extends IntegracaoBase {

    @Test
    @DisplayName("Todas as migrations devem aplicar com sucesso (e o Hibernate validate subir o contexto)")
    void migracoesAplicadas() {
        Integer falhas = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = false", Integer.class);
        Integer aplicadas = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = true and version is not null", Integer.class);

        assertThat(falhas).isZero();
        assertThat(aplicadas).isGreaterThanOrEqualTo(9);
    }

    @Test
    @DisplayName("Os três perfis do enum devem existir, e nenhum usuário deve vir das migrations")
    void perfisSemUsuarios() {
        var perfis = jdbcTemplate.queryForList("select nome from perfis order by nome", String.class);

        assertThat(perfis).containsExactly("ADMINISTRATOR", "MANAGER", "OPERATOR");
    }

    @Test
    @DisplayName("Tabelas removidas pelas migrations não devem existir mais")
    void tabelasRemovidas() {
        Integer existentes = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public'
                  and table_name in ('identidades', 'contatos', 'enderecos', 'servico_peca_insumo')
                """, Integer.class);

        assertThat(existentes).isZero();
    }
}
