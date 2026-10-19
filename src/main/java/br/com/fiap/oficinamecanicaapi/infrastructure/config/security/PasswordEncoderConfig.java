package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Isolated so the password encoder does not live in the same configuration
 * class that depends on the JWT filter — otherwise IdentityPortImpl pulls
 * SecurityConfig back into the graph and creates a cycle.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
