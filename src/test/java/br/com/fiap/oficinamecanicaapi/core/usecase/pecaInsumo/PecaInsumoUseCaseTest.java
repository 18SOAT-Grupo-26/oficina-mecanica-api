package br.com.fiap.oficinamecanicaapi.core.usecase.pecaInsumo;

import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoRequest;
import br.com.fiap.oficinamecanicaapi.core.domain.pecaInsumo.model.PecaInsumoResponse;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.out.pecaInsumo.PecaInsumoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.pecaInsumo.mapper.PecaInsumoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PecaInsumoUseCaseTest {

    private static final Long ID = 1L;
    private static final PecaInsumoRequest REQUEST = new PecaInsumoRequest(
            "  Óleo do Motor  ", "Sintético 5W30", 10, 2, new BigDecimal("59.9"));

    @Mock
    private PecaInsumoPortOut pecaInsumoPortOut;

    private PecaInsumoUseCase useCase;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        useCase = new PecaInsumoUseCase(pecaInsumoPortOut, Mappers.getMapper(PecaInsumoMapper.class), meterRegistry);
    }

    @Test
    @DisplayName("criar persiste insumo com nome normalizado e preço com 2 casas")
    void criarPersisteInsumo() {
        when(pecaInsumoPortOut.existePorNomeNormalizado("oleo do motor")).thenReturn(false);
        when(pecaInsumoPortOut.salvar(any())).thenAnswer(inv -> {
            PecaInsumo e = inv.getArgument(0);
            e.setId(ID);
            return e;
        });

        PecaInsumoResponse response = useCase.criar(REQUEST);

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.nome()).isEqualTo("Óleo do Motor");
        assertThat(response.preco()).isEqualByComparingTo("59.90");
        assertThat(response.preco().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("criar rejeita nome já cadastrado (409)")
    void criarRejeitaNomeDuplicado() {
        when(pecaInsumoPortOut.existePorNomeNormalizado("oleo do motor")).thenReturn(true);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.criar(REQUEST));

        assertThat(ex.getErrorCode()).isEqualTo(409);
        verify(pecaInsumoPortOut, never()).excluir(any());
    }

    @Test
    @DisplayName("criar rejeita reservado maior que quantidade (422)")
    void criarRejeitaReservadoMaiorQueQuantidade() {
        PecaInsumoRequest invalido = new PecaInsumoRequest("Filtro", null, 2, 5, BigDecimal.TEN);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.criar(invalido));

        assertThat(ex.getErrorCode()).isEqualTo(422);
    }

    @Test
    @DisplayName("listar aplica paginação padrão e limita o tamanho máximo")
    void listarAplicaLimites() {
        PecaInsumo entity = entidade(ID, "Filtro");
        when(pecaInsumoPortOut.listar(0, 20)).thenReturn(new Pagina<>(List.of(entity), 0, 20, 1));
        when(pecaInsumoPortOut.listar(0, 100)).thenReturn(new Pagina<>(List.of(), 0, 100, 0));

        Pagina<PecaInsumoResponse> padrao = useCase.listar(null, null);
        useCase.listar(-1, 500);

        assertThat(padrao.itens()).hasSize(1);
        assertThat(padrao.itens().get(0).nome()).isEqualTo("Filtro");
        verify(pecaInsumoPortOut).listar(0, 100);
    }

    @Test
    @DisplayName("buscarPorId retorna insumo")
    void buscarPorIdRetornaInsumo() {
        when(pecaInsumoPortOut.buscarPorId(ID)).thenReturn(Optional.of(entidade(ID, "Filtro")));

        assertThat(useCase.buscarPorId(ID).nome()).isEqualTo("Filtro");
    }

    @Test
    @DisplayName("buscarPorId lança 404 quando não existe")
    void buscarPorIdNaoEncontrado() {
        when(pecaInsumoPortOut.buscarPorId(ID)).thenReturn(Optional.empty());

        ApplicationException ex = assertThrows(ApplicationException.class, () -> useCase.buscarPorId(ID));

        assertThat(ex.getErrorCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("atualizar mantém o próprio nome sem checar duplicidade")
    void atualizarMesmoNome() {
        PecaInsumo entity = entidade(ID, "Filtro de Ar");
        when(pecaInsumoPortOut.buscarPorId(ID)).thenReturn(Optional.of(entity));
        when(pecaInsumoPortOut.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        PecaInsumoResponse response = useCase.atualizar(ID,
                new PecaInsumoRequest("filtro de ar", "novo", 7, 0, new BigDecimal("30.00")));

        assertThat(response.quantidade()).isEqualTo(7);
        assertThat(response.descricao()).isEqualTo("novo");
        verify(pecaInsumoPortOut, never()).existePorNomeNormalizado(any());
    }

    @Test
    @DisplayName("atualizar com nome de outro insumo lança 409")
    void atualizarNomeDuplicado() {
        when(pecaInsumoPortOut.buscarPorId(ID)).thenReturn(Optional.of(entidade(ID, "Filtro")));
        when(pecaInsumoPortOut.existePorNomeNormalizado("correia")).thenReturn(true);

        ApplicationException ex = assertThrows(ApplicationException.class, () ->
                useCase.atualizar(ID, new PecaInsumoRequest("Correia", null, 1, 0, BigDecimal.ONE)));

        assertThat(ex.getErrorCode()).isEqualTo(409);
    }

    @Test
    @DisplayName("excluir remove o insumo")
    void excluirRemoveInsumo() {
        PecaInsumo entity = entidade(ID, "Filtro");
        when(pecaInsumoPortOut.buscarPorId(ID)).thenReturn(Optional.of(entity));

        useCase.excluir(ID);

        verify(pecaInsumoPortOut).excluir(entity);
    }

    @Test
    @DisplayName("excluir lança 404 para insumo inexistente")
    void excluirNaoEncontrado() {
        when(pecaInsumoPortOut.buscarPorId(ID)).thenReturn(Optional.empty());

        assertThrows(ApplicationException.class, () -> useCase.excluir(ID));
        verify(pecaInsumoPortOut, never()).excluir(any());
    }

    private static PecaInsumo entidade(Long id, String nome) {
        PecaInsumo e = new PecaInsumo();
        e.setId(id);
        e.setNome(nome);
        e.setNomeNormalizado(nome.toLowerCase());
        e.setQuantidade(5);
        e.setReservado(0);
        e.setPreco(new BigDecimal("10.00"));
        return e;
    }

    static Stream<Arguments> requestsInvalidos() {
        String descricaoLonga = "x".repeat(501);
        return Stream.of(
                Arguments.of(request(null, 10, 0, "10"), "O campo nome é obrigatório"),
                Arguments.of(request("Ab", 10, 0, "10"), "O nome deve ter no mínimo 3 caracteres"),
                Arguments.of(request("Filtro", null, 0, "10"), "O campo quantidade é obrigatório"),
                Arguments.of(request("Filtro", -1, 0, "10"), "Quantidade inválida"),
                Arguments.of(request("Filtro", 10, 0, null), "O campo preco é obrigatório"),
                Arguments.of(request("Filtro", 10, 0, "0"), "Preço inválido"),
                Arguments.of(request("Filtro", 10, 0, "10.999"), "Preço inválido"),
                Arguments.of(request("Filtro", 10, -1, "10"),
                        "Quantidade reservada inválida. Deve ser entre 0 e a quantidade em estoque"),
                Arguments.of(new PecaInsumoRequest("Filtro", descricaoLonga, 10, 0, new BigDecimal("10")),
                        "A descrição deve ter no máximo 500 caracteres"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("requestsInvalidos")
    @DisplayName("criar responde 422 para dados inválidos, sem consultar a persistência")
    void criarValidaCampos(PecaInsumoRequest request, String mensagem) {
        assertThatThrownBy(() -> useCase.criar(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessage(mensagem)
                .extracting("errorCode").isEqualTo(422);
        verifyNoInteractions(pecaInsumoPortOut);
    }

    @Test
    @DisplayName("criar sem reservado informado grava reservado zero e conta a operação")
    void criarComReservadoNulo() {
        when(pecaInsumoPortOut.existePorNomeNormalizado("vela de ignicao")).thenReturn(false);
        when(pecaInsumoPortOut.salvar(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        PecaInsumoResponse response = useCase.criar(request("Vela de ignição", 8, null, "20"));

        assertThat(response.reservado()).isZero();
        assertThat(operacoes("criado")).isEqualTo(1);
    }

    @Test
    @DisplayName("atualizar ajusta quantidade e preço e conta a operação")
    void atualizarQuantidadeEPreco() {
        PecaInsumo atual = peca("Filtro de óleo", "filtro de oleo", 10, 2, "30.00");
        when(pecaInsumoPortOut.buscarPorId(ID)).thenReturn(Optional.of(atual));
        when(pecaInsumoPortOut.salvar(atual)).thenReturn(atual);

        PecaInsumoResponse response = useCase.atualizar(ID, request("Filtro de óleo", 25, 2, "32.5"));

        assertThat(response.quantidade()).isEqualTo(25);
        assertThat(response.preco()).isEqualByComparingTo("32.50");
        verify(pecaInsumoPortOut, never()).existePorNomeNormalizado(any());
        assertThat(operacoes("atualizado")).isEqualTo(1);
    }

    @Test
    @DisplayName("atualizar trocando o nome confere a duplicidade do nome novo")
    void atualizarTrocandoNome() {
        PecaInsumo atual = peca("Filtro de óleo", "filtro de oleo", 10, 0, "30.00");
        when(pecaInsumoPortOut.buscarPorId(ID)).thenReturn(Optional.of(atual));
        when(pecaInsumoPortOut.existePorNomeNormalizado("filtro de ar")).thenReturn(false);
        when(pecaInsumoPortOut.salvar(atual)).thenReturn(atual);

        assertThat(useCase.atualizar(ID, request("Filtro de ar", 10, 0, "30")).nome()).isEqualTo("Filtro de ar");
    }

    @Test
    @DisplayName("atualizar insumo inexistente responde 404")
    void atualizarInexistente() {
        when(pecaInsumoPortOut.buscarPorId(ID)).thenReturn(Optional.empty());

        var alteracao = request("Filtro", 1, 0, "1");

        assertThatThrownBy(() -> useCase.atualizar(ID, alteracao))
                .isInstanceOf(ApplicationException.class)
                .extracting("errorCode").isEqualTo(404);

        verify(pecaInsumoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("excluir conta a operação")
    void excluirContaOperacao() {
        PecaInsumo atual = peca("Filtro de óleo", "filtro de oleo", 10, 0, "30.00");
        when(pecaInsumoPortOut.buscarPorId(ID)).thenReturn(Optional.of(atual));

        useCase.excluir(ID);

        verify(pecaInsumoPortOut).excluir(atual);
        assertThat(operacoes("excluido")).isEqualTo(1);
    }

    private static PecaInsumoRequest request(String nome, Integer quantidade, Integer reservado, String preco) {
        return new PecaInsumoRequest(nome, null, quantidade, reservado, preco == null ? null : new BigDecimal(preco));
    }

    private static PecaInsumo peca(String nome, String nomeNormalizado, int quantidade, int reservado,
                                         String preco) {
        PecaInsumo peca = new PecaInsumo();
        peca.setId(ID);
        peca.setNome(nome);
        peca.setNomeNormalizado(nomeNormalizado);
        peca.setQuantidade(quantidade);
        peca.setReservado(reservado);
        peca.setPreco(new BigDecimal(preco));
        return peca;
    }

    private double operacoes(String operacao) {
        var contador = meterRegistry.find("oficina.cadastro.operacoes")
                .tags("entidade", "peca_insumo", "operacao", operacao).counter();
        return contador == null ? 0 : contador.count();
    }
}
