package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.user.UserUseCasePortIn;
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
    private final UserUseCasePortIn userPortOut;

    public JwtAuthenticationFilter(JwtTokenProviderUseCase jwtTokenProvider,
                                   UserUseCasePortIn userPortOut) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userPortOut = userPortOut;
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
            if (Boolean.TRUE.equals(jwtTokenProvider.validateToken(token))) {
                String username = jwtTokenProvider.extractUsername(token);
                var userDetails = userPortOut.retrieveUser(
                        SigninRequest.builder().identifier(username).build());

                List<GrantedAuthority> authorities = new ArrayList<>();

                // Role como authority (ex: ROLE_ADMINISTRATOR)
                authorities.add(userDetails.user().role());

                // Permissions como authorities individuais (ex: TUTOR_CREATE)
                if (userDetails.permissions() != null) {
                    userDetails.permissions().forEach(p ->
                            authorities.add(new SimpleGrantedAuthority(p)));
                }

                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities);
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