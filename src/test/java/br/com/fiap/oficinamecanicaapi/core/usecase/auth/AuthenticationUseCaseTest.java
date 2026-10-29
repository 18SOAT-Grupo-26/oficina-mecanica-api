package br.com.fiap.oficinamecanicaapi.core.usecase.auth;

import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.AuthToken;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.CadastroRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.auth.model.SigninRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.DetalhesUsuario;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Perfil;
import br.com.fiap.oficinamecanicaapi.core.domain.usuario.model.Usuario;
import br.com.fiap.oficinamecanicaapi.core.port.in.usuario.UsuarioUseCasePortIn;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Login sem enumeração de usuários: usuário inexistente e senha errada respondem igual (401).
 * Também cobre cadastro, validação do token e o contador oficina.auth.login por resultado.
 * Usa @InjectMocks e mocks dos records para não depender dos construtores, que mudam com o modelo de usuário.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationUseCaseTest {

    private static final String IDENTIFICADOR = "admin";
    private static final String SENHA = "Admin@123";
    private static final String SENHA_CIFRADA = "{bcrypt}cifrada";
    private static final String TOKEN = "jwt";
    private static final String MENSAGEM = "Credenciais inválidas";
    private static final String HASH_FICTICIO = "{bcrypt}ficticio";
    /** Acima dos 72 bytes que o bcrypt aceita no encode. */
    private static final String SENHA_LONGA = "A".repeat(100);
    private static final Long ID_CLIENTE = 10L;
    private static final SigninRequest REQUEST = new SigninRequest(IDENTIFICADOR, SENHA);
    // ADMIN não tem cliente ligado: clienteId nulo
    private static final CadastroRequest CADASTRO = new CadastroRequest("Ana Admin", "ana@oficina.com.br", "ana",
            "Admin@123", false, Perfil.ADMIN, null);
    private static final Usuario USUARIO = new Usuario("Ana Admin", "ana@oficina.com.br", "ana",
            "Admin@123", false, Perfil.ADMIN);

    @Mock
    private UsuarioUseCasePortIn usuarioService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProviderUseCase jwtTokenProvider;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();
    @Mock
    private LimiteTentativasLogin limiteTentativas;

    @InjectMocks
    private AuthenticationUseCase useCase;

    // ---- login ------------------------------------------------------------------------------------

    @Test
    @DisplayName("Deve gerar o token quando usuário e senha conferem")
    void deveAutenticar() {
        var detalhes = detalhesComSenha(SENHA_CIFRADA);
        when(usuarioService.buscarPorIdentificador(IDENTIFICADOR)).thenReturn(detalhes);
        when(passwordEncoder.matches(SENHA, SENHA_CIFRADA)).thenReturn(true);
        when(jwtTokenProvider.gerarToken(detalhes)).thenReturn(TOKEN);

        AuthToken authToken = useCase.autenticar(REQUEST);

        assertThat(authToken.token()).isEqualTo("Bearer " + TOKEN);
        assertThat(logins("sucesso")).isEqualTo(1);
        verify(limiteTentativas).registrarSucesso(IDENTIFICADOR);
        verify(limiteTentativas, never()).registrarFalha(IDENTIFICADOR);
    }

    @Test
    @DisplayName("Deve responder 401 com a mensagem genérica quando a senha estiver errada")
    void deveRecusarSenhaErrada() {
        var detalhes = detalhesComSenha(SENHA_CIFRADA);
        when(usuarioService.buscarPorIdentificador(IDENTIFICADOR)).thenReturn(detalhes);
        when(passwordEncoder.matches(SENHA, SENHA_CIFRADA)).thenReturn(false);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.autenticar(REQUEST));

        assertThat(ex.getErrorCode()).isEqualTo(401);
        assertThat(ex.getMessage()).isEqualTo(MENSAGEM);
        assertThat(logins("senha_invalida")).isEqualTo(1);
        verifyNoInteractions(jwtTokenProvider);
        verify(limiteTentativas).registrarFalha(IDENTIFICADOR);
    }

    @Test
    @DisplayName("Deve responder igual à senha errada (401) quando o usuário não existir, sem revelar isso")
    void deveRecusarUsuarioInexistenteComoSenhaErrada() {
        when(usuarioService.buscarPorIdentificador(IDENTIFICADOR))
                .thenThrow(new ApplicationException(404, "Usuário não encontrado"));
        when(passwordEncoder.encode(AuthenticationUseCase.SENHA_FICTICIA)).thenReturn(HASH_FICTICIO);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.autenticar(REQUEST));

        assertThat(ex.getErrorCode()).isEqualTo(401);
        assertThat(ex.getMessage()).isEqualTo(MENSAGEM);
        // custo de bcrypt equivalente ao da senha errada: o tempo de resposta também não denuncia
        verify(passwordEncoder).matches(SENHA, HASH_FICTICIO);
        assertThat(logins("usuario_inexistente")).isEqualTo(1);
        verifyNoInteractions(jwtTokenProvider);
        // conta a falha também para quem não existe: o 429 não revela quais contas existem
        verify(limiteTentativas).registrarFalha(IDENTIFICADOR);
    }

    @Test
    @DisplayName("Deve responder 429 sem consultar usuário nem senha quando o identificador estiver bloqueado")
    void deveResponder429QuandoBloqueado() {
        doThrow(new ApplicationException(429, "Muitas tentativas de login. Tente novamente em alguns minutos."))
                .when(limiteTentativas).verificarBloqueio(IDENTIFICADOR);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.autenticar(REQUEST));

        assertThat(ex.getErrorCode()).isEqualTo(429);
        verifyNoInteractions(usuarioService, passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("Deve propagar erros que não sejam de usuário inexistente")
    void devePropagarOutrosErros() {
        when(usuarioService.buscarPorIdentificador(IDENTIFICADOR))
                .thenThrow(new ApplicationException(500, "Erro interno inesperado"));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.autenticar(REQUEST));

        assertThat(ex.getErrorCode()).isEqualTo(500);
        verify(passwordEncoder, never()).encode(SENHA);
        assertThat(logins("usuario_inexistente")).isZero();
    }

    // ---- cadastro ---------------------------------------------------------------------------------

    @Test
    @DisplayName("Deve cadastrar repassando os dados do request e contar o cadastro por perfil")
    void deveCadastrar() {
        useCase.cadastrarUsuario(CADASTRO);

        verify(usuarioService).cadastrar(USUARIO);
        assertThat(meterRegistry.get("oficina.auth.cadastro").tag("perfil", "ADMIN").counter().count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Deve repassar o cliente ligado ao cadastrar um usuário CLIENTE")
    void deveCadastrarClienteComClienteLigado() {
        var cadastroCliente = new CadastroRequest("Maria Silva", "maria@email.com", "maria",
                "Cliente@123", false, Perfil.CLIENTE, ID_CLIENTE);

        useCase.cadastrarUsuario(cadastroCliente);

        verify(usuarioService).cadastrar(new Usuario("Maria Silva", "maria@email.com", "maria",
                "Cliente@123", false, Perfil.CLIENTE, ID_CLIENTE));
        assertThat(meterRegistry.get("oficina.auth.cadastro").tag("perfil", "CLIENTE").counter().count())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Deve propagar o erro de negócio do cadastro sem contar")
    void devePropagarErroDeNegocioNoCadastro() {
        doThrow(new ApplicationException(409, "Já existe um usuário cadastrado com este e-mail"))
                .when(usuarioService).cadastrar(USUARIO);

        assertThatThrownBy(() -> useCase.cadastrarUsuario(CADASTRO))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(409);
        assertThat(meterRegistry.find("oficina.auth.cadastro").counter()).isNull();
    }

    @Test
    @DisplayName("Deve traduzir falha inesperada do cadastro em 500 sem expor a causa")
    void deveTraduzirFalhaInesperadaNoCadastro() {
        doThrow(new IllegalStateException("conexão perdida")).when(usuarioService).cadastrar(USUARIO);

        assertThatThrownBy(() -> useCase.cadastrarUsuario(CADASTRO))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Erro ao cadastrar o usuário")
                .extracting("errorCode").isEqualTo(500);
    }

    // ---- validação do token -----------------------------------------------------------------------

    @Test
    @DisplayName("Deve tirar o prefixo Bearer antes de validar o token")
    void deveValidarTokenSemPrefixo() {
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(true);

        assertThat(useCase.validarToken("Bearer " + TOKEN)).isTrue();
    }

    @Test
    @DisplayName("Deve validar o token também quando vier sem o prefixo Bearer")
    void deveValidarTokenCru() {
        when(jwtTokenProvider.validarToken(TOKEN)).thenReturn(false);

        assertThat(useCase.validarToken(TOKEN)).isFalse();
    }

    @Test
    @DisplayName("Deve propagar o 401 do token expirado ou inválido")
    void devePropagar401() {
        when(jwtTokenProvider.validarToken(TOKEN)).thenThrow(new ApplicationException(401, "Token inválido"));

        assertThatThrownBy(() -> useCase.validarToken("Bearer " + TOKEN))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(401);
    }

    @Test
    @DisplayName("Deve traduzir falha inesperada na validação em 500")
    void deveTraduzirFalhaInesperadaNaValidacao() {
        when(jwtTokenProvider.validarToken(TOKEN)).thenThrow(new IllegalStateException("falha"));

        assertThatThrownBy(() -> useCase.validarToken("Bearer " + TOKEN))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("Erro ao validar o token JWT")
                .extracting("errorCode").isEqualTo(500);
    }

    // ---- apoio ------------------------------------------------------------------------------------

    private double logins(String resultado) {
        var contador = meterRegistry.find("oficina.auth.login").tag("resultado", resultado).counter();
        return contador == null ? 0 : contador.count();
    }

    private static DetalhesUsuario detalhesComSenha(String senhaCifrada) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.senha()).thenReturn(senhaCifrada);
        DetalhesUsuario detalhes = mock(DetalhesUsuario.class);
        when(detalhes.usuario()).thenReturn(usuario);
        return detalhes;
    }

    @Test
    @DisplayName("Senha acima de 72 bytes deve responder 401 igual para usuário inexistente e existente (bcrypt real)")
    void senhaLongaNaoDeveRevelarUsuarioInexistente() {
        PasswordEncoder bcrypt = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        AuthenticationUseCase comBcryptReal = new AuthenticationUseCase(usuarioService, bcrypt, jwtTokenProvider,
                limiteTentativas, new SimpleMeterRegistry());
        SigninRequest inexistente = new SigninRequest("ninguem", SENHA_LONGA);
        SigninRequest existente = new SigninRequest(IDENTIFICADOR, SENHA_LONGA);
        when(usuarioService.buscarPorIdentificador("ninguem"))
                .thenThrow(new ApplicationException(404, "Usuário não encontrado"));
        var detalhes = detalhesComSenha(bcrypt.encode(SENHA));
        when(usuarioService.buscarPorIdentificador(IDENTIFICADOR)).thenReturn(detalhes);

        ApplicationException doInexistente = assertThrows(ApplicationException.class,
                () -> comBcryptReal.autenticar(inexistente));
        ApplicationException doExistente = assertThrows(ApplicationException.class,
                () -> comBcryptReal.autenticar(existente));

        assertThat(doInexistente.getErrorCode()).isEqualTo(401);
        assertThat(doExistente.getErrorCode()).isEqualTo(401);
        assertThat(doInexistente.getMessage()).isEqualTo(doExistente.getMessage());
    }
}