package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.auth.JwtTokenProviderUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProviderUseCase jwtTokenProvider;
    private final UsuarioUseCasePortIn usuarioUseCase;

    public JwtAuthenticationFilter(JwtTokenProviderUseCase jwtTokenProvider,
                                   UsuarioUseCasePortIn usuarioUseCase) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.usuarioUseCase = usuarioUseCase;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            if (Boolean.TRUE.equals(jwtTokenProvider.validarToken(token))) {
                String nomeUsuario = jwtTokenProvider.extrairNomeUsuario(token);
                var detalhesUsuario = usuarioUseCase.buscarPorIdentificador(nomeUsuario);

                List<GrantedAuthority> authorities = new ArrayList<>();

                // Perfil como authority (ex: ROLE_ADMINISTRATOR)
                authorities.add(detalhesUsuario.usuario().perfil());

                // Permissões como authorities individuais (ex: TUTOR_CREATE)
                if (detalhesUsuario.permissoes() != null) {
                    detalhesUsuario.permissoes().forEach(p ->
                            authorities.add(new SimpleGrantedAuthority(p)));
                }

                var auth = new UsernamePasswordAuthenticationToken(
                        detalhesUsuario, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Throwable ex) {
            log.debug("JWT authentication skipped: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}