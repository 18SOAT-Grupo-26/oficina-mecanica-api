package br.com.fiap.oficinamecanicaapi.core.usecase.veiculo;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Placa;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Veiculo;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.veiculo.VeiculoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.veiculo.VeiculoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.veiculo.mapper.VeiculoMapper;
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
class VeiculoUseCaseTest {

    private static final Long ID = 5L;
    private static final Long CLIENTE_ID = 1L;
    private static final Long OUTRO_CLIENTE_ID = 2L;
    private static final String PLACA = "BRA2E19";
    private static final String PLACA_COM_HIFEN = "bra-2e19";
    private static final String OUTRA_PLACA = "ABC1234";
    private static final String MARCA = "Volkswagen";
    private static final String MODELO = "Gol";
    private static final Integer ANO = 2020;
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-22T10:00:00-03:00");
    private static final VeiculoRequest REQUEST = new VeiculoRequest(PLACA_COM_HIFEN, MARCA, MODELO, ANO, CLIENTE_ID);
    private static final Cliente CLIENTE = Cliente.reconstituir(CLIENTE_ID, "Maria da Silva",
            new Documento("52998224725"), "maria@email.com", "11999998888", null, true, CRIADO_EM, CRIADO_EM);

    @Mock
    private VeiculoPortOut veiculoPortOut;

    @Mock
    private ClientePortOut clientePortOut;

    @Captor
    private ArgumentCaptor<Veiculo> veiculoCaptor;

    private VeiculoUseCase useCase;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        useCase = new VeiculoUseCase(veiculoPortOut, clientePortOut, Mappers.getMapper(VeiculoMapper.class), meterRegistry);
    }

    @Test
    @DisplayName("Deve criar veículo ativo com placa normalizada para cliente ativo")
    void deveCriarVeiculo() {
        Placa placa = new Placa(PLACA);
        when(clientePortOut.buscarAtivoPorId(CLIENTE_ID)).thenReturn(Optional.of(CLIENTE));
        when(veiculoPortOut.existePorPlaca(placa)).thenReturn(false);
        when(veiculoPortOut.salvar(any(Veiculo.class))).thenReturn(veiculoSalvo(placa, CLIENTE_ID));

        VeiculoResponse response = useCase.criar(REQUEST);

        verify(veiculoPortOut).salvar(veiculoCaptor.capture());
        assertThat(veiculoCaptor.getValue().getPlaca()).isEqualTo(placa);
        assertThat(veiculoCaptor.getValue().isAtivo()).isTrue();
        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.placa()).isEqualTo(PLACA);
        assertThat(response.clienteId()).isEqualTo(CLIENTE_ID);
    }

    @Test
    @DisplayName("Deve responder 404 ao criar veículo para cliente inexistente ou inativo")
    void deveRejeitarClienteInexistenteNoCadastro() {
        when(clientePortOut.buscarAtivoPorId(CLIENTE_ID)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.criar(REQUEST));

        assertThat(exception.getErrorCode()).isEqualTo(404);
        assertThat(exception.getMessage()).isEqualTo("Cliente não encontrado");
        verify(veiculoPortOut, never()).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve responder 409 ao criar veículo com placa já cadastrada")
    void deveRejeitarPlacaDuplicadaNoCadastro() {
        when(clientePortOut.buscarAtivoPorId(CLIENTE_ID)).thenReturn(Optional.of(CLIENTE));
        when(veiculoPortOut.existePorPlaca(new Placa(PLACA))).thenReturn(true);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.criar(REQUEST));

        assertThat(exception.getErrorCode()).isEqualTo(409);
        verify(veiculoPortOut, never()).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve responder 400 com placa inválida sem consultar a persistência")
    void deveRejeitarPlacaInvalida() {
        VeiculoRequest request = new VeiculoRequest("ABCD123", MARCA, MODELO, ANO, CLIENTE_ID);

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.criar(request));

        assertThat(exception.getErrorCode()).isEqualTo(400);
        verify(clientePortOut, never()).buscarAtivoPorId(CLIENTE_ID);
    }

    @Test
    @DisplayName("Deve buscar veículo ativo por id")
    void deveBuscarPorId() {
        when(veiculoPortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(veiculoSalvo(new Placa(PLACA), CLIENTE_ID)));

        assertThat(useCase.buscarPorId(ID).modelo()).isEqualTo(MODELO);
    }

    @Test
    @DisplayName("Deve responder 404 ao buscar veículo inexistente por id")
    void deveRetornarNaoEncontradoPorId() {
        when(veiculoPortOut.buscarAtivoPorId(ID)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.buscarPorId(ID));

        assertThat(exception.getErrorCode()).isEqualTo(404);
        assertThat(exception.getMessage()).isEqualTo("Veículo não encontrado");
    }

    @Test
    @DisplayName("Deve buscar veículo por placa informada com hífen e minúsculas")
    void deveBuscarPorPlaca() {
        Placa placa = new Placa(PLACA);
        when(veiculoPortOut.buscarAtivoPorPlaca(placa)).thenReturn(Optional.of(veiculoSalvo(placa, CLIENTE_ID)));

        assertThat(useCase.buscarPorPlaca(PLACA_COM_HIFEN).placa()).isEqualTo(PLACA);
    }

    @Test
    @DisplayName("Deve responder 404 ao buscar veículo inexistente por placa")
    void deveRetornarNaoEncontradoPorPlaca() {
        when(veiculoPortOut.buscarAtivoPorPlaca(new Placa(PLACA))).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> useCase.buscarPorPlaca(PLACA));

        assertThat(exception.getErrorCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Deve listar veículos do cliente com paginação informada")
    void deveListarPorCliente() {
        Veiculo veiculo = veiculoSalvo(new Placa(PLACA), CLIENTE_ID);
        when(veiculoPortOut.listarAtivos(CLIENTE_ID, 1, 5)).thenReturn(new Pagina<>(List.of(veiculo), 1, 5, 6));

        Pagina<VeiculoResponse> pagina = useCase.listar(CLIENTE_ID, 1, 5);

        assertThat(pagina.itens()).extracting(VeiculoResponse::id).containsExactly(ID);
        assertThat(pagina.total()).isEqualTo(6);
    }

    @Test
    @DisplayName("Deve listar todos os veículos com paginação padrão quando nada for informado")
    void deveListarTodosComPaginacaoPadrao() {
        when(veiculoPortOut.listarAtivos(null, Pagina.PAGINA_PADRAO, Pagina.TAMANHO_PADRAO))
                .thenReturn(new Pagina<>(List.of(), Pagina.PAGINA_PADRAO, Pagina.TAMANHO_PADRAO, 0));

        assertThat(useCase.listar(null, null, null).itens()).isEmpty();
    }

    @Test
    @DisplayName("Deve atualizar veículo mantendo placa e cliente sem revalidá-los")
    void deveAtualizarMantendoPlacaECliente() {
        Veiculo veiculo = veiculoSalvo(new Placa(PLACA), CLIENTE_ID);
        VeiculoRequest request = new VeiculoRequest(PLACA, MARCA, "Gol G5", 2021, CLIENTE_ID);
        when(veiculoPortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(veiculo));
        when(veiculoPortOut.salvar(veiculo)).thenReturn(veiculo);

        VeiculoResponse response = useCase.atualizar(ID, request);

        assertThat(response.modelo()).isEqualTo("Gol G5");
        assertThat(response.ano()).isEqualTo(2021);
        verify(veiculoPortOut, never()).existePorPlaca(any(Placa.class));
        verify(clientePortOut, never()).buscarAtivoPorId(CLIENTE_ID);
    }

    @Test
    @DisplayName("Deve atualizar veículo trocando placa e cliente após validá-los")
    void deveAtualizarTrocandoPlacaECliente() {
        Veiculo veiculo = veiculoSalvo(new Placa(PLACA), CLIENTE_ID);
        Placa novaPlaca = new Placa(OUTRA_PLACA);
        VeiculoRequest request = new VeiculoRequest(OUTRA_PLACA, MARCA, MODELO, ANO, OUTRO_CLIENTE_ID);
        when(veiculoPortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(veiculo));
        when(clientePortOut.buscarAtivoPorId(OUTRO_CLIENTE_ID)).thenReturn(Optional.of(CLIENTE));
        when(veiculoPortOut.existePorPlaca(novaPlaca)).thenReturn(false);
        when(veiculoPortOut.salvar(veiculo)).thenReturn(veiculo);

        VeiculoResponse response = useCase.atualizar(ID, request);

        assertThat(response.placa()).isEqualTo(OUTRA_PLACA);
        assertThat(response.clienteId()).isEqualTo(OUTRO_CLIENTE_ID);
    }

    @Test
    @DisplayName("Deve responder 409 ao atualizar para placa já cadastrada")
    void deveRejeitarPlacaDuplicadaNaAtualizacao() {
        when(veiculoPortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(veiculoSalvo(new Placa(PLACA), CLIENTE_ID)));
        when(veiculoPortOut.existePorPlaca(new Placa(OUTRA_PLACA))).thenReturn(true);
        VeiculoRequest request = new VeiculoRequest(OUTRA_PLACA, MARCA, MODELO, ANO, CLIENTE_ID);

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> useCase.atualizar(ID, request));

        assertThat(exception.getErrorCode()).isEqualTo(409);
        verify(veiculoPortOut, never()).salvar(any(Veiculo.class));
    }

    @Test
    @DisplayName("Deve responder 404 ao transferir veículo para cliente inexistente")
    void deveRejeitarClienteInexistenteNaAtualizacao() {
        when(veiculoPortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(veiculoSalvo(new Placa(PLACA), CLIENTE_ID)));
        when(clientePortOut.buscarAtivoPorId(OUTRO_CLIENTE_ID)).thenReturn(Optional.empty());
        VeiculoRequest request = new VeiculoRequest(PLACA, MARCA, MODELO, ANO, OUTRO_CLIENTE_ID);

        ApplicationException exception = assertThrows(ApplicationException.class,
                () -> useCase.atualizar(ID, request));

        assertThat(exception.getErrorCode()).isEqualTo(404);
        assertThat(exception.getMessage()).isEqualTo("Cliente não encontrado");
    }

    @Test
    @DisplayName("Deve desativar veículo (soft delete)")
    void deveDesativarVeiculo() {
        when(veiculoPortOut.buscarAtivoPorId(ID)).thenReturn(Optional.of(veiculoSalvo(new Placa(PLACA), CLIENTE_ID)));

        useCase.desativar(ID);

        verify(veiculoPortOut).salvar(veiculoCaptor.capture());
        assertThat(veiculoCaptor.getValue().isAtivo()).isFalse();
    }

    @Test
    @DisplayName("Deve responder 404 ao desativar veículo inexistente")
    void deveRetornarNaoEncontradoNaDesativacao() {
        when(veiculoPortOut.buscarAtivoPorId(ID)).thenReturn(Optional.empty());

        ApplicationException exception = assertThrows(ApplicationException.class, () -> useCase.desativar(ID));

        assertThat(exception.getErrorCode()).isEqualTo(404);
        verify(veiculoPortOut, never()).salvar(any(Veiculo.class));
    }

    private static Veiculo veiculoSalvo(Placa placa, Long clienteId) {
        return Veiculo.reconstituir(ID, placa, MARCA, MODELO, ANO, clienteId, true, CRIADO_EM, CRIADO_EM);
    }
}
