package br.com.fiap.oficinamecanicaapi.core.usecase.cliente;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.ClienteResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Endereco;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.TipoDocumento;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.cliente.mapper.ClienteMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteUseCaseTest {

    private static final Long ID = 1L;
    private static final String NOME = "Maria da Silva";
    private static final String CPF = "52998224725";
    private static final String CPF_COM_MASCARA = "529.982.247-25";
    private static final String CNPJ = "12ABC34501DE35";
    private static final String EMAIL = "maria@email.com";
    private static final String TELEFONE = "11999998888";
    private static final String NOVO_NOME = "Maria Souza";
    private static final Endereco ENDERECO = new Endereco("Rua A", "100", "Centro", "São Paulo", "SP",
            "01001000", null);
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-21T10:00:00-03:00");
    private static final ClienteRequest REQUEST = new ClienteRequest(NOME, CPF_COM_MASCARA, EMAIL, TELEFONE, ENDERECO);

    @Mock
    private ClientePortOut clientePortOut;

    @Captor
    private ArgumentCaptor<Cliente> clienteCaptor;

    private ClienteUseCase useCase;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        useCase = new ClienteUseCase(clientePortOut, Mappers.getMapper(ClienteMapper.class), meterRegistry);
    }

    @Test
    @DisplayName("Deve criar cliente ativo com documento normalizado")
    void deveCriarCliente() {
        Documento documento = new Documento(CPF);
        when(clientePortOut.existePorDocumento(documento)).thenReturn(false);
        when(clientePortOut.salvar(any(Cliente.class))).thenReturn(clienteSalvo(documento));

        ClienteResponse response = useCase.criar(REQUEST);

        verify(clientePortOut).salvar(clienteCaptor.capture());
        Cliente enviado = clienteCaptor.getValue();
        assertThat(enviado.getId()).isNull();
        assertThat(enviado.isAtivo()).isTrue();
        assertThat(enviado.getDocumento()).isEqualTo(documento);
        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.documento()).isEqualTo(CPF);
        assertThat(response.tipoDocumento()).isEqualTo(TipoDocumento.CPF);
        assertThat(response.endereco()).isEqualTo(ENDERECO);
    }

    @Test
    @DisplayName("Deve rejeitar cadastro com documento já usado com erro 409")
    void deveRejeitarDocumentoDuplicadoNoCadastro() {
        when(clientePortOut.existePorDocumento(new Documento(CPF))).thenReturn(true);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.criar(REQUEST));

        assertThat(exception.getErrorCode()).isEqualTo(409);
        verify(clientePortOut, never()).salvar(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve rejeitar cadastro com e-mail inválido com erro 400")
    void deveRejeitarEmailInvalido() {
        ClienteRequest request = new ClienteRequest(NOME, CPF, "maria-sem-arroba", TELEFONE, null);
        when(clientePortOut.existePorDocumento(new Documento(CPF))).thenReturn(false);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.criar(request));

        assertThat(exception.getErrorCode()).isEqualTo(400);
        verify(clientePortOut, never()).salvar(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve rejeitar cadastro sem nome com erro 400")
    void deveRejeitarNomeEmBranco() {
        ClienteRequest request = new ClienteRequest(" ", CPF, EMAIL, TELEFONE, null);
        when(clientePortOut.existePorDocumento(new Documento(CPF))).thenReturn(false);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.criar(request));

        assertThat(exception.getErrorCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve rejeitar cadastro com documento inválido com erro 400 sem consultar a persistência")
    void deveRejeitarDocumentoInvalido() {
        ClienteRequest request = new ClienteRequest(NOME, "12345678900", EMAIL, TELEFONE, null);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.criar(request));

        assertThat(exception.getErrorCode()).isEqualTo(400);
        verify(clientePortOut, never()).existePorDocumento(any(Documento.class));
    }

    @Test
    @DisplayName("Deve buscar cliente ativo por id")
    void deveBuscarPorId() {
        when(clientePortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(clienteSalvo(new Documento(CPF))));

        ClienteResponse response = useCase.buscarPorId(ID);

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.nome()).isEqualTo(NOME);
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar cliente inexistente ou inativo por id")
    void deveRetornarNaoEncontradoPorId() {
        when(clientePortOut.buscarAtivoPorId(ID)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.buscarPorId(ID));

        assertThat(exception.getErrorCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Deve buscar cliente por documento informado com máscara")
    void deveBuscarPorDocumento() {
        Documento documento = new Documento(CPF);
        when(clientePortOut.buscarAtivoPorDocumento(documento)).thenReturn(Optional.of(clienteSalvo(documento)));

        ClienteResponse response = useCase.buscarPorDocumento(CPF_COM_MASCARA);

        assertThat(response.documento()).isEqualTo(CPF);
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar cliente inexistente por documento")
    void deveRetornarNaoEncontradoPorDocumento() {
        when(clientePortOut.buscarAtivoPorDocumento(new Documento(CPF))).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> useCase.buscarPorDocumento(CPF));

        assertThat(exception.getErrorCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Deve listar clientes ativos com página e tamanho informados")
    void deveListarComPaginacaoInformada() {
        Cliente cliente = clienteSalvo(new Documento(CPF));
        when(clientePortOut.listarAtivos(2, 10)).thenReturn(new Pagina<>(List.of(cliente), 2, 10, 21));

        Pagina<ClienteResponse> pagina = useCase.listar(2, 10);

        assertThat(pagina.itens()).extracting(ClienteResponse::id).containsExactly(ID);
        assertThat(pagina.total()).isEqualTo(21);
    }

    @Test
    @DisplayName("Deve usar paginação padrão quando página e tamanho não forem informados ou forem inválidos")
    void deveUsarPaginacaoPadrao() {
        when(clientePortOut.listarAtivos(Pagina.PAGINA_PADRAO, Pagina.TAMANHO_PADRAO))
                .thenReturn(new Pagina<>(List.of(), Pagina.PAGINA_PADRAO, Pagina.TAMANHO_PADRAO, 0));

        assertThat(useCase.listar(null, null).itens()).isEmpty();
        assertThat(useCase.listar(-1, 0).itens()).isEmpty();
    }

    @Test
    @DisplayName("Deve limitar o tamanho da página ao máximo permitido")
    void deveLimitarTamanhoMaximo() {
        when(clientePortOut.listarAtivos(0, Pagina.TAMANHO_MAXIMO))
                .thenReturn(new Pagina<>(List.of(), 0, Pagina.TAMANHO_MAXIMO, 0));

        Pagina<ClienteResponse> pagina = useCase.listar(0, 500);

        assertThat(pagina.tamanho()).isEqualTo(Pagina.TAMANHO_MAXIMO);
    }

    @Test
    @DisplayName("Deve atualizar cliente mantendo o mesmo documento sem checar duplicidade")
    void deveAtualizarMantendoDocumento() {
        Cliente cliente = clienteSalvo(new Documento(CPF));
        ClienteRequest request = new ClienteRequest(NOVO_NOME, CPF_COM_MASCARA, EMAIL, TELEFONE, ENDERECO);
        when(clientePortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(cliente));
        when(clientePortOut.salvar(cliente)).thenReturn(cliente);

        ClienteResponse response = useCase.atualizar(ID, request);

        assertThat(response.nome()).isEqualTo(NOVO_NOME);
        verify(clientePortOut, never()).existePorDocumento(any(Documento.class));
    }

    @Test
    @DisplayName("Deve atualizar cliente para outro documento disponível")
    void deveAtualizarParaNovoDocumento() {
        Cliente cliente = clienteSalvo(new Documento(CPF));
        Documento novoDocumento = new Documento(CNPJ);
        ClienteRequest request = new ClienteRequest(NOME, CNPJ, EMAIL, TELEFONE, null);
        when(clientePortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(cliente));
        when(clientePortOut.existePorDocumento(novoDocumento)).thenReturn(false);
        when(clientePortOut.salvar(cliente)).thenReturn(cliente);

        ClienteResponse response = useCase.atualizar(ID, request);

        assertThat(response.documento()).isEqualTo(CNPJ);
        assertThat(response.tipoDocumento()).isEqualTo(TipoDocumento.CNPJ);
    }

    @Test
    @DisplayName("Deve rejeitar atualização para documento já usado com erro 409")
    void deveRejeitarDocumentoDuplicadoNaAtualizacao() {
        Cliente cliente = clienteSalvo(new Documento(CPF));
        ClienteRequest request = new ClienteRequest(NOME, CNPJ, EMAIL, TELEFONE, null);
        when(clientePortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(cliente));
        when(clientePortOut.existePorDocumento(new Documento(CNPJ))).thenReturn(true);

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> useCase.atualizar(ID, request));

        assertThat(exception.getErrorCode()).isEqualTo(409);
        verify(clientePortOut, never()).salvar(any(Cliente.class));
    }

    @Test
    @DisplayName("Deve retornar 404 ao atualizar cliente inexistente")
    void deveRetornarNaoEncontradoNaAtualizacao() {
        when(clientePortOut.buscarAtivoPorId(ID)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> useCase.atualizar(ID, REQUEST));

        assertThat(exception.getErrorCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Deve desativar cliente (soft delete)")
    void deveDesativarCliente() {
        Cliente cliente = clienteSalvo(new Documento(CPF));
        when(clientePortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(cliente));

        useCase.desativar(ID);

        verify(clientePortOut).salvar(clienteCaptor.capture());
        assertThat(clienteCaptor.getValue().isAtivo()).isFalse();
    }

    @Test
    @DisplayName("Deve retornar 404 ao desativar cliente inexistente")
    void deveRetornarNaoEncontradoNaDesativacao() {
        when(clientePortOut.buscarAtivoPorId(ID)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.desativar(ID));

        assertThat(exception.getErrorCode()).isEqualTo(404);
        verify(clientePortOut, never()).salvar(any(Cliente.class));
    }

    private static Cliente clienteSalvo(Documento documento) {
        return Cliente.reconstituir(ID, NOME, documento, EMAIL, TELEFONE, ENDERECO, true, CRIADO_EM, CRIADO_EM);
    }
}
