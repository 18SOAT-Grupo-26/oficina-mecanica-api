package br.com.fiap.oficinamecanicaapi.core.usecase.servico;

import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Pagina;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.servico.ServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.out.servico.ServicoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.servico.mapper.ServicoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ConflitoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.DominioException;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoUseCaseTest {

    private static final Long ID = 1L;

    @Mock
    private ServicoPortOut servicoPortOut;

    /** Mapper real (MapStruct) espionado: os testes que precisam fixam a resposta com when(...). */
    @Spy
    private ServicoMapper servicoMapper = Mappers.getMapper(ServicoMapper.class);

    private ServicoUseCase useCase;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        useCase = new ServicoUseCase(servicoPortOut, servicoMapper, meterRegistry);
    }

    private static ServicoRequest request(String nome, String descricao, String preco) {
        return new ServicoRequest(nome, descricao, preco == null ? null : new BigDecimal(preco));
    }

    @Test
    @DisplayName("criar persiste serviço com nome normalizado e preço com 2 casas")
    void criarPersisteServico() {
        when(servicoPortOut.existePorNomeNormalizado("troca de oleo")).thenReturn(false);
        when(servicoPortOut.salvar(any())).thenAnswer(inv -> {
            Servico e = inv.getArgument(0);
            // como o banco: devolve o serviço gravado, agora com id
            return Servico.reconstituir(ID, e.getNome(), e.getNomeNormalizado(), e.getDescricao(), e.getPreco(),
                    e.getCriadoEm(), e.getAtualizadoEm());
        });
        ServicoResponse esperado = new ServicoResponse(ID, "Troca de Óleo", null, new BigDecimal("80.00"), null, null);
        when(servicoMapper.toResponse(any())).thenReturn(esperado);

        ServicoResponse response = useCase.criar(request("  Troca de Óleo ", null, "80"));

        assertThat(response.id()).isEqualTo(ID);
        ArgumentCaptor<Servico> captor = ArgumentCaptor.forClass(Servico.class);
        verify(servicoPortOut).salvar(captor.capture());
        Servico salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Troca de Óleo");
        assertThat(salvo.getNomeNormalizado()).isEqualTo("troca de oleo");
        assertThat(salvo.getPreco().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("criar rejeita nome com menos de 3 caracteres (422)")
    void criarRejeitaNomeCurto() {
        DadoInvalidoException ex = assertThrows(DadoInvalidoException.class,
                () -> useCase.criar(request("ab", null, "10")));

        assertThat(ex.getMessage()).isEqualTo("O nome deve ter no mínimo 3 caracteres");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita nome duplicado ignorando maiúsculas, acentos e espaços (409)")
    void criarRejeitaNomeDuplicado() {
        when(servicoPortOut.existePorNomeNormalizado("troca de oleo")).thenReturn(true);

        ConflitoException ex = assertThrows(ConflitoException.class,
                () -> useCase.criar(request("  TROCA   de óleo", null, "10")));

        assertThat(ex.getMessage()).isEqualTo("Já existe um serviço cadastrado com este nome");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita preço zero ou negativo (422)")
    void criarRejeitaPrecoNaoPositivo() {
        DadoInvalidoException ex = assertThrows(DadoInvalidoException.class,
                () -> useCase.criar(request("Alinhamento", null, "0")));

        assertThat(ex.getMessage()).isEqualTo("Preço inválido");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita preço com mais de duas casas decimais (422)")
    void criarRejeitaPrecoComTresCasas() {
        DadoInvalidoException ex = assertThrows(DadoInvalidoException.class,
                () -> useCase.criar(request("Alinhamento", null, "10.123")));

        assertThat(ex.getMessage()).isEqualTo("Preço inválido");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("criar rejeita descrição com mais de 500 caracteres (422)")
    void criarRejeitaDescricaoLonga() {
        String descricao = "a".repeat(501);

        DadoInvalidoException ex = assertThrows(DadoInvalidoException.class,
                () -> useCase.criar(request("Alinhamento", descricao, "10")));

        assertThat(ex.getMessage()).isEqualTo("A descrição deve ter no máximo 500 caracteres");
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("buscarPorId retorna 404 quando serviço não existe")
    void buscarPorIdNaoEncontrado() {
        when(servicoPortOut.buscarPorId(ID)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException ex = assertThrows(RecursoNaoEncontradoException.class, () -> useCase.buscarPorId(ID));

    }

    @Test
    @DisplayName("listar aplica a paginação padrão quando nada é informado")
    void listarComPaginacaoPadrao() {
        when(servicoPortOut.listar(0, 20)).thenReturn(new Pagina<>(List.of(servico("Troca de óleo", "80.00")), 0, 20, 1));

        Pagina<ServicoResponse> pagina = useCase.listar(null, null);

        assertThat(pagina.itens()).extracting(ServicoResponse::nome).containsExactly("Troca de óleo");
        assertThat(pagina.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("listar corrige página negativa e limita o tamanho a 100")
    void listarCorrigePaginacao() {
        when(servicoPortOut.listar(0, 100)).thenReturn(new Pagina<>(List.of(), 0, 100, 0));

        assertThat(useCase.listar(-3, 500).tamanho()).isEqualTo(100);
    }

    @Test
    @DisplayName("buscarPorId devolve o serviço")
    void buscarPorId() {
        when(servicoPortOut.buscarPorId(ID)).thenReturn(Optional.of(servico("Troca de óleo", "80.00")));

        assertThat(useCase.buscarPorId(ID).preco()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("atualizar mantendo o nome não confere duplicidade, troca o preço e conta a operação")
    void atualizarMesmoNome() {
        Servico atual = servico("Troca de óleo", "80.00");
        when(servicoPortOut.buscarPorId(ID)).thenReturn(Optional.of(atual));
        when(servicoPortOut.salvar(atual)).thenReturn(atual);

        ServicoResponse response = useCase.atualizar(ID, new ServicoRequest("TROCA DE ÓLEO", "Com filtro",
                new BigDecimal("95.5")));

        assertThat(response.preco()).isEqualByComparingTo("95.50");
        assertThat(response.descricao()).isEqualTo("Com filtro");
        assertThat(atual.getPreco().scale()).isEqualTo(2);
        verify(servicoPortOut, never()).existePorNomeNormalizado(anyString());
        assertThat(operacoes("atualizado")).isEqualTo(1);
    }

    @Test
    @DisplayName("atualizar para o nome de outro serviço responde 409 sem gravar")
    void atualizarNomeDuplicado() {
        when(servicoPortOut.buscarPorId(ID)).thenReturn(Optional.of(servico("Troca de óleo", "80.00")));
        when(servicoPortOut.existePorNomeNormalizado("alinhamento")).thenReturn(true);

        assertErro(ConflitoException.class, "Já existe um serviço cadastrado com este nome",
                () -> useCase.atualizar(ID, new ServicoRequest("Alinhamento", null, new BigDecimal("50"))));
        verify(servicoPortOut, never()).salvar(any());
    }

    @Test
    @DisplayName("atualizar serviço inexistente responde 404 sem validar o request")
    void atualizarInexistente() {
        when(servicoPortOut.buscarPorId(ID)).thenReturn(Optional.empty());

        assertErro(RecursoNaoEncontradoException.class, "Serviço não encontrado", () -> useCase.atualizar(ID, new ServicoRequest(null, null, null)));
    }

    @Test
    @DisplayName("excluir remove o serviço e conta a operação")
    void excluir() {
        Servico atual = servico("Troca de óleo", "80.00");
        when(servicoPortOut.buscarPorId(ID)).thenReturn(Optional.of(atual));

        useCase.excluir(ID);

        verify(servicoPortOut).excluir(atual);
        assertThat(operacoes("excluido")).isEqualTo(1);
    }

    @Test
    @DisplayName("excluir serviço inexistente responde 404")
    void excluirInexistente() {
        when(servicoPortOut.buscarPorId(ID)).thenReturn(Optional.empty());

        assertErro(RecursoNaoEncontradoException.class, "Serviço não encontrado", () -> useCase.excluir(ID));
        verify(servicoPortOut, never()).excluir(any());
    }

    @Test
    @DisplayName("criar conta a operação no sucesso e não conta quando a validação falha")
    void criarContaOperacao() {
        when(servicoPortOut.existePorNomeNormalizado("balanceamento")).thenReturn(false);
        when(servicoPortOut.salvar(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        useCase.criar(new ServicoRequest("Balanceamento", null, new BigDecimal("60")));
        var semPreco = new ServicoRequest("Balanceamento", null, null);
        assertThatThrownBy(() -> useCase.criar(semPreco))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(operacoes("criado")).isEqualTo(1);
    }

    @ParameterizedTest(name = "nome=[{0}] preco={1} → {2}")
    @CsvSource(value = {
            "NULL, 10, O campo nome é obrigatório",
            "'   ', 10, O campo nome é obrigatório",
            "Ab, 10, O nome deve ter no mínimo 3 caracteres",
            "Alinhamento, NULL, O campo preco é obrigatório",
            "Alinhamento, -1, Preço inválido"}, nullValues = "NULL")
    @DisplayName("criar responde 422 para nome ou preço inválidos, sem consultar a persistência")
    void criarValidaCampos(String nome, String preco, String mensagem) {
        ServicoRequest request = new ServicoRequest(nome, null, preco == null ? null : new BigDecimal(preco));

        assertErro(DadoInvalidoException.class, mensagem, () -> useCase.criar(request));
        verifyNoInteractions(servicoPortOut);
    }

    private static Servico servico(String nome, String preco) {
        Servico servico = Servico.reconstituir(ID, nome, "troca de oleo", null, new BigDecimal(preco), null, null);
        return servico;
    }

    private double operacoes(String operacao) {
        var contador = meterRegistry.find("oficina.cadastro.operacoes")
                .tags("entidade", "servico", "operacao", operacao).counter();
        return contador == null ? 0 : contador.count();
    }

    private static void assertErro(Class<? extends DominioException> tipo, String mensagem,
                                   org.assertj.core.api.ThrowableAssert.ThrowingCallable acao) {
        assertThatThrownBy(acao)
                .isInstanceOf(tipo)
                .hasMessage(mensagem);
    }
}
