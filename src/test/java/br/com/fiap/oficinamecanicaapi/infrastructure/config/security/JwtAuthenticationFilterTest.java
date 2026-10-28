package br.com.fiap.oficinamecanicaapi.infrastructure.config.security;

import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.core.usecase.auth.JwtTokenProviderUseCase;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "jwt";
    private static final Usuario USUARIO = new Usuario("Ana", "ana@oficina.com.br", "ana", null, false, Perfil.MANAGER);

    @Mock
    private JwtTokenProviderUseCase jwtTokenProvider;

    @Mock
    private UsuarioUseCasePortIn usuarioUseCase;

    private JwtAuthenticationFilter filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filtro = new JwtAuthenticationFilter(jwtTokenProvider, usuarioUseCase);
        request = new MockHttpServletRequest("GET", "/clientes");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sem header Authorization deve seguir a cadeia sem autenticar e sem olhar token")
    void semHeader() throws Exception {
        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(jwtTokenProvider, usuarioUseCase);
    }

    @Test
    @DisplayName("Header que não é Bearer deve ser ignorado")
    void headerSemBearer() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46c2VuaGE=");

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtTokenProvider, usuarioUseCase);
    }

    @Test
    @DisplayName("Token válido deve autenticar com o perfil (ROLE_) e as permissões como authorities")
    void tokenValido() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        DetalhesUsuario detalhes = new DetalhesUsuario(USUARIO, Set.of("OS_LER"));
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extrairNomeUsuario(TOKEN)).thenReturn("ana");
        when(usuarioUseCase.buscarPorIdentificador("ana")).thenReturn(detalhes);

        filtro.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(detalhes);
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_MANAGER", "OS_LER");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("Usuário sem permissões deve receber só a authority do perfil")
    void semPermissoes() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extrairNomeUsuario(TOKEN)).thenReturn("ana");
        when(usuarioUseCase.buscarPorIdentificador("ana")).thenReturn(new DetalhesUsuario(USUARIO, null));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_MANAGER");
    }

    @Test
    @DisplayName("Token recusado (expirado ou inválido) deve deixar a requisição sem autenticação, sem barrar a cadeia")
    void tokenRecusado() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtTokenProvider.validarToken(TOKEN)).thenThrow(new ApplicationException(401, "Token expirado. Faça login novamente."));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(usuarioUseCase);
    }

    @Test
    @DisplayName("Token de usuário que não existe mais não deve autenticar")
    void usuarioRemovido() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.extrairNomeUsuario(TOKEN)).thenReturn("ana");
        when(usuarioUseCase.buscarPorIdentificador("ana")).thenThrow(new ApplicationException(404, "Usuário não encontrado"));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Validação que devolve falso não deve autenticar nem extrair o usuário")
    void validacaoFalsa() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(false);

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).extrairNomeUsuario(anyString());
    }
}
