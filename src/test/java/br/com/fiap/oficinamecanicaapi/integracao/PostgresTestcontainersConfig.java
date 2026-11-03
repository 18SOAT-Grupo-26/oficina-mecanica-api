package br.com.fiap.oficinamecanicaapi.integracao;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Um PostgreSQL real por contexto de teste. O @ServiceConnection preenche url/usuário/senha do datasource,
 * e o Flyway roda as mesmas migrations da aplicação.
 *
 * <p>Testcontainers 1.x: troque o import por org.testcontainers.containers.PostgreSQLContainer
 * e o tipo por PostgreSQLContainer&lt;?&gt;.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18-alpine");
    }
}
