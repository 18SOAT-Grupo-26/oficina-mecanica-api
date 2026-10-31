package br.com.fiap.oficinamecanicaapi.core.usecase.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.cliente.model.Cliente;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.*;
import br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model.PecaInsumo;
import br.com.fiap.oficinamecanicaapi.core.domain.servico.model.Servico;
import br.com.fiap.oficinamecanicaapi.core.domain.shared.model.Documento;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Placa;
import br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model.Veiculo;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoRequest;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.OrdemServicoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.in.ordemservico.TempoMedioExecucaoResponse;
import br.com.fiap.oficinamecanicaapi.core.port.out.cliente.ClientePortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.ordemservico.OrdemServicoPortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.pecainsumo.PecaInsumoPortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.servico.ServicoPortOut;
import br.com.fiap.oficinamecanicaapi.core.port.out.veiculo.VeiculoPortOut;
import br.com.fiap.oficinamecanicaapi.core.usecase.ordemservico.mapper.OrdemServicoMapper;
import br.com.fiap.oficinamecanicaapi.shared.exception.ConflitoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import br.com.fiap.oficinamecanicaapi.shared.exception.DominioException;
import br.com.fiap.oficinamecanicaapi.shared.exception.RecursoNaoEncontradoException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdemServicoUseCaseTest {

    private static final OffsetDateTime ABERTURA = OffsetDateTime.parse("2026-10-24T07:30:00-03:00");
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-10-24T08:00:00-03:00");
    private static final Clock CLOCK = Clock.fixed(Instant.from(AGORA), ZoneOffset.of("-03:00"));
    private static final Long ID_OS = 99L;
    private static final Long ID_CLIENTE = 10L;
    private static final Long ID_OUTRO_CLIENTE = 11L;
    private static final Long ID_VEICULO = 20L;
    private static final Long ID_SERVICO = 7L;
    private static final Long ID_ALINHAMENTO = 8L;
    private static final Long ID_PECA = 3L;
    private static final Long ID_VELA = 4L;
    private static final String DOCUMENTO = "529.982.247-25";
    private static final String OUTRO_DOCUMENTO = "111.444.777-35";
    private static final Cliente CLIENTE = Cliente.reconstituir(ID_CLIENTE, "Maria da Silva",
            new Documento(DOCUMENTO), "maria@email.com", "11999998888", null, true, ABERTURA, ABERTURA);
    private static final Cliente OUTRO_CLIENTE = Cliente.reconstituir(ID_OUTRO_CLIENTE, "João Souza",
            new Documento(OUTRO_DOCUMENTO), "joao@email.com", "11988887777", null, true, ABERTURA, ABERTURA);

    @Mock
    private OrdemServicoPortOut ordemServicoPortOut;

    @Mock
    private ClientePortOut clientePortOut;

    @Mock
    private VeiculoPortOut veiculoPortOut;

    @Mock
    private ServicoPortOut servicoPortOut;

    @Mock
    private PecaInsumoPortOut pecaInsumoPortOut;

    @Captor
    private ArgumentCaptor<OrdemServico> osCaptor;

    private MeterRegistry meterRegistry;

    private OrdemServicoUseCase useCase;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        useCase = new OrdemServicoUseCase(ordemServicoPortOut, clientePortOut, veiculoPortOut, servicoPortOut,
                pecaInsumoPortOut, Mappers.getMapper(OrdemServicoMapper.class), CLOCK, meterRegistry);
    }

    @Nested
    @DisplayName("H7 — Criação da OS")
    class Criacao {

        @Test
        @DisplayName("Deve criar a OS com cliente e veículo e levá-la direto para EM_DIAGNOSTICO com a data/hora do relógio")
        void deveCriarOs() {
            clienteEVeiculoCadastrados();
            retornarOsSalva();

            OrdemServicoResponse response = useCase.criar(new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(), List.of()));

            verify(ordemServicoPortOut).salvar(osCaptor.capture());
            assertThat(osCaptor.getValue().getHistorico()).containsExactly(
                    new RegistroHistorico(null, RECEBIDA, AGORA),
                    new RegistroHistorico(RECEBIDA, EM_DIAGNOSTICO, AGORA));
            assertThat(response.status()).isEqualTo(EM_DIAGNOSTICO);
            assertThat(response.idCliente()).isEqualTo(ID_CLIENTE);
            assertThat(response.idVeiculo()).isEqualTo(ID_VEICULO);
            assertThat(response.historico()).hasSize(2);
        }

        @Test
        @DisplayName("Deve responder 400 quando falta cliente ou veículo, sem consultar os cadastros")
        void deveResponder400SemCamposObrigatorios() {
            assertErro(DadoInvalidoException.class, "O cliente da OS é obrigatório", () -> useCase.criar(new OrdemServicoRequest(null, ID_VEICULO, List.of(), List.of())));
            assertErro(DadoInvalidoException.class, "O cliente da OS é obrigatório", () -> useCase.criar(new OrdemServicoRequest(" ", ID_VEICULO, List.of(), List.of())));
            assertErro(DadoInvalidoException.class, "O veículo da OS é obrigatório", () -> useCase.criar(new OrdemServicoRequest(DOCUMENTO, null, List.of(), List.of())));
            assertErro(DadoInvalidoException.class, "Os serviços da OS são obrigatórios (envie uma lista vazia se não houver)",
                    () -> useCase.criar(new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, null, List.of())));
            assertErro(DadoInvalidoException.class, "As peças da OS são obrigatórias (envie uma lista vazia se não houver)",
                    () -> useCase.criar(new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(), null)));
            assertErro(DadoInvalidoException.class, "Os dados da OS são obrigatórios", () -> useCase.criar(null));
            verifyNoInteractions(clientePortOut, veiculoPortOut, ordemServicoPortOut);
        }

        @Test
        @DisplayName("Deve criar a OS com itens dos cadastros, congelando o preço, sem reservar peças")
        void deveCriarComItens() {
            clienteEVeiculoCadastrados();
            when(servicoPortOut.buscarPorId(ID_SERVICO)).thenReturn(Optional.of(servico(ID_SERVICO, "Troca de óleo", "100")));
            when(pecaInsumoPortOut.buscarPorId(ID_PECA)).thenReturn(Optional.of(filtroComSaldo(2)));
            retornarOsSalva();

            OrdemServicoResponse response = useCase.criar(new OrdemServicoRequest(DOCUMENTO, ID_VEICULO,
                    List.of(new OrdemServicoRequest.Servico(ID_SERVICO, 1)), List.of(new OrdemServicoRequest.Peca(ID_PECA, 2))));

            assertThat(response.status()).isEqualTo(EM_DIAGNOSTICO);
            assertThat(response.itensServico()).singleElement().satisfies(item -> {
                assertThat(item.descricao()).isEqualTo("Troca de óleo");
                assertThat(item.precoUnitario()).isEqualByComparingTo("100");
            });
            assertThat(response.itensPeca()).singleElement().satisfies(item -> {
                assertThat(item.quantidade()).isEqualTo(2);
                assertThat(item.subtotal()).isEqualTo(new BigDecimal("60.00"));
            });
        }

        @Test
        @DisplayName("Deve responder 404 quando o serviço ou a peça não estão cadastrados")
        void deveResponder404ParaItemSemCadastro() {
            clienteEVeiculoCadastrados();
            when(servicoPortOut.buscarPorId(ID_SERVICO)).thenReturn(Optional.empty());
            when(pecaInsumoPortOut.buscarPorId(ID_PECA)).thenReturn(Optional.empty());

            assertErro(RecursoNaoEncontradoException.class, "Serviço não encontrado no catálogo", () -> useCase.criar(
                    new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(new OrdemServicoRequest.Servico(ID_SERVICO, 1)), List.of())));
            assertErro(RecursoNaoEncontradoException.class, "Peça não encontrada no catálogo", () -> useCase.criar(
                    new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(), List.of(new OrdemServicoRequest.Peca(ID_PECA, 1)))));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve responder 409 quando a quantidade passa do saldo disponível")
        void deveResponder409ParaSaldoInsuficiente() {
            clienteEVeiculoCadastrados();
            when(pecaInsumoPortOut.buscarPorId(ID_PECA)).thenReturn(Optional.of(filtroComSaldo(1)));

            assertErro(ConflitoException.class, "Saldo insuficiente para a peça Filtro de óleo: disponível 1, solicitado 2", () -> useCase.criar(
                    new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(), List.of(new OrdemServicoRequest.Peca(ID_PECA, 2)))));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve responder 409 quando o mesmo item aparece duas vezes na requisição")
        void deveResponder409ParaItemRepetido() {
            clienteEVeiculoCadastrados();

            assertErro(ConflitoException.class, "Serviço repetido na requisição: " + ID_SERVICO, () -> useCase.criar(
                    new OrdemServicoRequest(DOCUMENTO, ID_VEICULO,
                            List.of(new OrdemServicoRequest.Servico(ID_SERVICO, 1), new OrdemServicoRequest.Servico(ID_SERVICO, 2)), List.of())));
            assertErro(ConflitoException.class, "Peça repetida na requisição: " + ID_PECA, () -> useCase.criar(
                    new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(),
                            List.of(new OrdemServicoRequest.Peca(ID_PECA, 1), new OrdemServicoRequest.Peca(ID_PECA, 2)))));
            verifyNoInteractions(servicoPortOut, pecaInsumoPortOut);
        }

        @Test
        @DisplayName("Deve responder 404 quando não há cliente ativo com o documento")
        void deveResponder404ParaClienteInexistente() {
            when(clientePortOut.buscarAtivoPorDocumento(any())).thenReturn(Optional.empty());

            assertErro(RecursoNaoEncontradoException.class, "Cliente não encontrado", () -> useCase.criar(new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(), List.of())));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve responder 400 para documento inválido sem consultar o cliente")
        void deveResponder400ParaDocumentoInvalido() {
            DadoInvalidoException exception = assertThrows(DadoInvalidoException.class,
                    () -> useCase.criar(new OrdemServicoRequest("123", ID_VEICULO, List.of(), List.of())));

            verify(clientePortOut, never()).buscarAtivoPorDocumento(any());
        }

        @Test
        @DisplayName("Deve responder 404 quando o veículo não existe")
        void deveResponder404ParaVeiculoInexistente() {
            when(clientePortOut.buscarAtivoPorDocumento(any())).thenReturn(Optional.of(CLIENTE));
            when(veiculoPortOut.buscarAtivoPorId(ID_VEICULO)).thenReturn(Optional.empty());

            assertErro(RecursoNaoEncontradoException.class, "Veículo não encontrado", () -> useCase.criar(new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(), List.of())));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve responder 409 quando o veículo é de outro cliente")
        void deveResponder409ParaVeiculoDeOutroCliente() {
            when(clientePortOut.buscarAtivoPorDocumento(any())).thenReturn(Optional.of(CLIENTE));
            when(veiculoPortOut.buscarAtivoPorId(ID_VEICULO)).thenReturn(Optional.of(veiculoDoCliente(ID_OUTRO_CLIENTE)));

            assertErro(ConflitoException.class, "O veículo não pertence ao cliente da OS",
                    () -> useCase.criar(new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(), List.of())));
            verify(ordemServicoPortOut, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("Consulta da OS")
    class Consulta {

        @Test
        @DisplayName("Deve consultar a OS com status e histórico, sem gravar nada")
        void deveConsultarOs() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));

            OrdemServicoResponse response = useCase.consultar(ID_OS);

            assertThat(response.id()).isEqualTo(ID_OS);
            assertThat(response.status()).isEqualTo(AGUARDANDO_APROVACAO);
            assertThat(response.itensPeca()).hasSize(2);
            assertThat(response.historico()).hasSize(1);
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve consultar a OS do próprio cliente")
        void deveConsultarOsDoCliente() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));

            OrdemServicoResponse response = useCase.consultarDoCliente(ID_OS, ID_CLIENTE);

            assertThat(response.id()).isEqualTo(ID_OS);
        }

        @Test
        @DisplayName("Deve responder 404 quando o cliente consulta a OS de outro cliente")
        void deveEsconderOsDeOutroCliente() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));

            assertErro(RecursoNaoEncontradoException.class, "Ordem de serviço não encontrada", () -> useCase.consultarDoCliente(ID_OS, ID_OUTRO_CLIENTE));
        }

        @Test
        @DisplayName("Deve responder 404 quando a OS não existe, ao consultar, ao atualizar e ao mudar o status")
        void deveResponder404ParaOsInexistente() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.empty());

            assertErro(RecursoNaoEncontradoException.class, "Ordem de serviço não encontrada", () -> useCase.consultar(ID_OS));
            assertErro(RecursoNaoEncontradoException.class, "Ordem de serviço não encontrada", () -> useCase.atualizar(ID_OS, putComItensAtuais(DOCUMENTO)));
            assertErro(RecursoNaoEncontradoException.class, "Ordem de serviço não encontrada", () -> useCase.alterarStatus(ID_OS, mudarStatus(FINALIZADA)));
            verify(ordemServicoPortOut, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("H7 — Atualização dos dados")
    class AtualizacaoDosDados {

        @Test
        @DisplayName("Deve responder 400 no PUT sem serviços ou peças, sem consultar a OS")
        void deveRecusarPutSemItens() {
            assertErro(DadoInvalidoException.class, "Os serviços da OS são obrigatórios (envie uma lista vazia se não houver)",
                    () -> useCase.atualizar(ID_OS, new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, null, List.of())));
            assertErro(DadoInvalidoException.class, "As peças da OS são obrigatórias (envie uma lista vazia se não houver)",
                    () -> useCase.atualizar(ID_OS, new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(), null)));
            verifyNoInteractions(ordemServicoPortOut, clientePortOut, veiculoPortOut);
        }

        @Test
        @DisplayName("Deve aceitar o PUT completo com os mesmos dados, sem mexer em nada nem consultar o veículo")
        void deveIgnorarDadosIguais() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            clienteCadastrado();
            retornarOsSalva();

            OrdemServicoResponse response = useCase.atualizar(ID_OS, putComItensAtuais(DOCUMENTO));

            assertThat(response.status()).isEqualTo(EM_DIAGNOSTICO);
            assertThat(response.itensPeca()).hasSize(2);
            verifyNoInteractions(veiculoPortOut, servicoPortOut, pecaInsumoPortOut);
        }

        @Test
        @DisplayName("Deve remover os itens quando o PUT manda listas vazias")
        void deveRemoverItensComListasVazias() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            clienteCadastrado();
            retornarOsSalva();

            OrdemServicoResponse response = useCase.atualizar(ID_OS,
                    new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(), List.of()));

            assertThat(response.itensServico()).isEmpty();
            assertThat(response.itensPeca()).isEmpty();
        }

        @Test
        @DisplayName("Deve incluir, remover e mudar a quantidade dos itens, mantendo o preço já congelado")
        void deveSincronizarItens() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            clienteCadastrado();
            when(servicoPortOut.buscarPorId(ID_ALINHAMENTO)).thenReturn(Optional.of(servico(ID_ALINHAMENTO, "Alinhamento", "80")));
            when(pecaInsumoPortOut.buscarPorId(ID_PECA)).thenReturn(Optional.of(filtroComSaldo(5)));
            retornarOsSalva();

            OrdemServicoResponse response = useCase.atualizar(ID_OS, new OrdemServicoRequest(DOCUMENTO, ID_VEICULO,
                    List.of(new OrdemServicoRequest.Servico(ID_SERVICO, 1), new OrdemServicoRequest.Servico(ID_ALINHAMENTO, 1)),
                    List.of(new OrdemServicoRequest.Peca(ID_PECA, 3))));

            assertThat(response.itensServico()).extracting(OrdemServicoResponse.ItemServico::idServico)
                    .containsExactlyInAnyOrder(ID_SERVICO, ID_ALINHAMENTO);
            assertThat(response.itensPeca()).singleElement().satisfies(item -> {
                assertThat(item.idPeca()).isEqualTo(ID_PECA);
                assertThat(item.quantidade()).isEqualTo(3);
                assertThat(item.precoUnitario()).isEqualByComparingTo("30");
            });
            verify(servicoPortOut, never()).buscarPorId(ID_SERVICO);
            verify(pecaInsumoPortOut, never()).buscarPorId(ID_VELA);
        }

        @Test
        @DisplayName("Deve responder 409 ao alterar itens com a OS aguardando aprovação")
        void deveRecusarAlterarItensAguardando() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));
            clienteCadastrado();

            assertErro(ConflitoException.class, "Itens só podem ser alterados com a OS em RECEBIDA ou EM_DIAGNOSTICO",
                    () -> useCase.atualizar(ID_OS, new OrdemServicoRequest(DOCUMENTO, ID_VEICULO,
                            List.of(new OrdemServicoRequest.Servico(ID_SERVICO, 1)), List.of())));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve responder 409 no PUT com outro cliente: o cliente da OS não muda depois da abertura")
        void deveRecusarTrocarCliente() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            when(clientePortOut.buscarAtivoPorDocumento(new Documento(OUTRO_DOCUMENTO))).thenReturn(Optional.of(OUTRO_CLIENTE));

            assertErro(ConflitoException.class, "O cliente da OS não pode ser alterado; cancele a OS e abra outra",
                    () -> useCase.atualizar(ID_OS, putComItensAtuais(OUTRO_DOCUMENTO)));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve responder 409 no PUT com outro veículo, sem consultar o cadastro de veículos")
        void deveRecusarTrocarVeiculo() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            clienteCadastrado();

            assertErro(ConflitoException.class, "O veículo da OS não pode ser alterado; cancele a OS e abra outra",
                    () -> useCase.atualizar(ID_OS, new OrdemServicoRequest(DOCUMENTO, 21L, List.of(), List.of())));
            verifyNoInteractions(veiculoPortOut);
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve recusar qualquer PUT a partir de EM_EXECUCAO, mesmo com os mesmos dados, sem consultar cadastros nem gravar")
        void deveRecusarEditarAPartirDeEmExecucao() {
            when(ordemServicoPortOut.buscarPorId(ID_OS))
                    .thenReturn(Optional.of(osNoStatus(EM_EXECUCAO)), Optional.of(osNoStatus(CANCELADA)));

            assertErro(ConflitoException.class, "A OS não pode ser editada com status EM_EXECUCAO",
                    () -> useCase.atualizar(ID_OS, putComItensAtuais(DOCUMENTO)));
            assertErro(ConflitoException.class, "A OS não pode ser editada com status CANCELADA",
                    () -> useCase.atualizar(ID_OS, putComItensAtuais(DOCUMENTO)));
            verifyNoInteractions(clientePortOut, veiculoPortOut, servicoPortOut, pecaInsumoPortOut);
            verify(ordemServicoPortOut, never()).salvar(any());
        }
    }

    @Nested
    @DisplayName("H8 — Mudança manual de status")
    class StatusManual {

        @Test
        @DisplayName("Deve recusar pedir EM_DIAGNOSTICO: a OS entra nele sozinha ao ser aberta")
        void deveRecusarEmDiagnosticoManual() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(RECEBIDA)));

            assertErro(ConflitoException.class, "EM_DIAGNOSTICO é automático: a OS entra nele ao ser aberta",
                    () -> useCase.alterarStatus(ID_OS, mudarStatus(EM_DIAGNOSTICO)));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve recusar pedir AGUARDANDO_APROVACAO: ela vem do envio do orçamento")
        void deveRecusarAguardandoAprovacaoManual() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));

            assertErro(ConflitoException.class, "AGUARDANDO_APROVACAO é automático: vem do envio do orçamento",
                    () -> useCase.alterarStatus(ID_OS, mudarStatus(AGUARDANDO_APROVACAO)));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve recusar pedir EM_EXECUCAO direto: ela vem da aprovação do orçamento")
        void deveRecusarEmExecucaoDireto() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));

            assertErro(ConflitoException.class, "EM_EXECUCAO é automático: vem da aprovação do orçamento",
                    () -> useCase.alterarStatus(ID_OS, mudarStatus(EM_EXECUCAO)));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve recusar pular status")
        void deveRecusarPularStatus() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));

            assertErro(ConflitoException.class, "Transição inválida: EM_DIAGNOSTICO → FINALIZADA",
                    () -> useCase.alterarStatus(ID_OS, mudarStatus(FINALIZADA)));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve finalizar e entregar")
        void deveFinalizarEEntregar() {
            when(ordemServicoPortOut.buscarPorId(ID_OS))
                    .thenReturn(Optional.of(osNoStatus(EM_EXECUCAO)), Optional.of(osNoStatus(FINALIZADA)));
            retornarOsSalva();

            assertThat(useCase.alterarStatus(ID_OS, mudarStatus(FINALIZADA)).status()).isEqualTo(FINALIZADA);
            assertThat(useCase.alterarStatus(ID_OS, mudarStatus(ENTREGUE)).status()).isEqualTo(ENTREGUE);
        }

        @Test
        @DisplayName("[EXTRA] Deve registrar no Timer oficina.os.tempo.execucao o tempo de EM_EXECUCAO até FINALIZADA")
        void deveRegistrarMetricaAoFinalizar() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osEmExecucaoDesde(AGORA.minusMinutes(90))));
            retornarOsSalva();

            useCase.alterarStatus(ID_OS, mudarStatus(FINALIZADA));

            Timer timer = meterRegistry.get("oficina.os.tempo.execucao").timer();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MINUTES)).isEqualTo(90.0);
        }

        @Test
        @DisplayName("[EXTRA] Não deve registrar a métrica em outras mudanças de status nem quando a finalização falha")
        void naoDeveRegistrarMetricaForaDaFinalizacao() {
            when(ordemServicoPortOut.buscarPorId(ID_OS))
                    .thenReturn(Optional.of(osNoStatus(FINALIZADA)), Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            retornarOsSalva();

            useCase.alterarStatus(ID_OS, mudarStatus(ENTREGUE));
            assertErro(ConflitoException.class, "Transição inválida: EM_DIAGNOSTICO → FINALIZADA",
                    () -> useCase.alterarStatus(ID_OS, mudarStatus(FINALIZADA)));

            assertThat(meterRegistry.get("oficina.os.tempo.execucao").timer().count()).isZero();
        }

        @Test
        @DisplayName("Deve cancelar em RECEBIDA, EM_DIAGNOSTICO e AGUARDANDO_APROVACAO, gravando o histórico")
        void deveCancelarAteAguardandoAprovacao() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(RECEBIDA)),
                    Optional.of(osNoStatus(EM_DIAGNOSTICO)), Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));
            when(pecaInsumoPortOut.liberar(ID_PECA, 2)).thenReturn(true);
            when(pecaInsumoPortOut.liberar(ID_VELA, 4)).thenReturn(true);
            retornarOsSalva();

            for (StatusOrdemServico anterior : List.of(RECEBIDA, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO)) {
                OrdemServicoResponse response = useCase.alterarStatus(ID_OS, mudarStatus(CANCELADA));

                assertThat(response.status()).isEqualTo(CANCELADA);
                assertThat(response.historico()).last()
                        .isEqualTo(new OrdemServicoResponse.RegistroHistorico(anterior, CANCELADA, AGORA));
            }
        }

        @Test
        @DisplayName("Deve liberar a reserva só ao cancelar com o orçamento pendente (AGUARDANDO_APROVACAO)")
        void deveLiberarReservaAoCancelarComOrcamentoPendente() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)),
                    Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));
            when(pecaInsumoPortOut.liberar(ID_PECA, 2)).thenReturn(true);
            when(pecaInsumoPortOut.liberar(ID_VELA, 4)).thenReturn(true);
            retornarOsSalva();

            useCase.alterarStatus(ID_OS, mudarStatus(CANCELADA));
            verifyNoInteractions(pecaInsumoPortOut);

            useCase.alterarStatus(ID_OS, mudarStatus(CANCELADA));
            verify(pecaInsumoPortOut).liberar(ID_PECA, 2);
            verify(pecaInsumoPortOut).liberar(ID_VELA, 4);
        }

        @Test
        @DisplayName("Deve cancelar pelo soft delete, liberando a reserva com o orçamento pendente")
        void deveCancelarPeloSoftDelete() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));
            when(pecaInsumoPortOut.liberar(ID_PECA, 2)).thenReturn(true);
            when(pecaInsumoPortOut.liberar(ID_VELA, 4)).thenReturn(true);
            retornarOsSalva();

            useCase.cancelar(ID_OS);

            verify(ordemServicoPortOut).salvar(argThat(os -> os.getStatus() == CANCELADA));
            verify(pecaInsumoPortOut).liberar(ID_PECA, 2);
            verify(pecaInsumoPortOut).liberar(ID_VELA, 4);
        }

        @Test
        @DisplayName("Deve recusar o soft delete a partir de EM_EXECUCAO e responder 404 para OS inexistente")
        void deveRecusarSoftDeleteInvalido() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_EXECUCAO)), Optional.empty());

            assertErro(ConflitoException.class, "Transição inválida: EM_EXECUCAO → CANCELADA", () -> useCase.cancelar(ID_OS));
            assertErro(RecursoNaoEncontradoException.class, "Ordem de serviço não encontrada", () -> useCase.cancelar(ID_OS));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve recusar cancelar a partir de EM_EXECUCAO sem gravar")
        void deveRecusarCancelarEmExecucao() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_EXECUCAO)));

            assertErro(ConflitoException.class, "Transição inválida: EM_EXECUCAO → CANCELADA",
                    () -> useCase.alterarStatus(ID_OS, mudarStatus(CANCELADA)));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve recusar voltar para RECEBIDA")
        void deveRecusarVoltarParaRecebida() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));

            assertErro(ConflitoException.class, "Transição inválida: EM_DIAGNOSTICO → RECEBIDA",
                    () -> useCase.alterarStatus(ID_OS, mudarStatus(RECEBIDA)));
        }

        @Test
        @DisplayName("Deve recusar pedir o status atual: só avança para o próximo")
        void deveRecusarRepetirStatusAtual() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(FINALIZADA)));

            assertErro(ConflitoException.class, "Transição inválida: FINALIZADA → FINALIZADA",
                    () -> useCase.alterarStatus(ID_OS, mudarStatus(FINALIZADA)));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve responder 400 quando o status de destino não vem, sem consultar a OS")
        void deveExigirStatusDeDestino() {
            assertErro(DadoInvalidoException.class, "O status de destino é obrigatório", () -> useCase.alterarStatus(ID_OS, mudarStatus(null)));
            assertErro(DadoInvalidoException.class, "O status de destino é obrigatório", () -> useCase.alterarStatus(ID_OS, null));
            verifyNoInteractions(ordemServicoPortOut);
        }
    }

    @Nested
    @DisplayName("H8 — Registros do orçamento (chamados pelo contexto de orçamento)")
    class RegistrosDoOrcamento {

        @Test
        @DisplayName("Deve ir para AGUARDANDO_APROVACAO quando o orçamento é enviado")
        void deveAguardarAprovacaoNoEnvio() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            when(pecaInsumoPortOut.reservar(ID_PECA, 2)).thenReturn(true);
            when(pecaInsumoPortOut.reservar(ID_VELA, 4)).thenReturn(true);
            retornarOsSalva();

            OrdemServicoResponse response = useCase.registrarOrcamentoEnviado(ID_OS);

            assertThat(response.status()).isEqualTo(AGUARDANDO_APROVACAO);
            assertThat(response.historico()).last()
                    .isEqualTo(new OrdemServicoResponse.RegistroHistorico(EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, AGORA));
            verify(pecaInsumoPortOut).reservar(ID_PECA, 2);
            verify(pecaInsumoPortOut).reservar(ID_VELA, 4);
        }

        @Test
        @DisplayName("Deve ir para EM_EXECUCAO automaticamente na aprovação, gravando o histórico")
        void deveIniciarExecucaoNaAprovacao() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));
            when(pecaInsumoPortOut.baixar(ID_PECA, 2)).thenReturn(true);
            when(pecaInsumoPortOut.baixar(ID_VELA, 4)).thenReturn(true);
            retornarOsSalva();

            OrdemServicoResponse response = useCase.registrarOrcamentoAprovado(ID_OS);

            verify(ordemServicoPortOut).salvar(any());
            assertThat(response.status()).isEqualTo(EM_EXECUCAO);
            assertThat(response.historico()).last()
                    .isEqualTo(new OrdemServicoResponse.RegistroHistorico(AGUARDANDO_APROVACAO, EM_EXECUCAO, AGORA));
            verify(pecaInsumoPortOut).baixar(ID_PECA, 2);
            verify(pecaInsumoPortOut).baixar(ID_VELA, 4);
        }

        @Test
        @DisplayName("Deve ir para CANCELADA automaticamente na rejeição, gravando o histórico")
        void deveCancelarNaRejeicao() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));
            when(pecaInsumoPortOut.liberar(ID_PECA, 2)).thenReturn(true);
            when(pecaInsumoPortOut.liberar(ID_VELA, 4)).thenReturn(true);
            retornarOsSalva();

            OrdemServicoResponse response = useCase.registrarOrcamentoRejeitado(ID_OS);

            assertThat(response.status()).isEqualTo(CANCELADA);
            assertThat(response.historico()).last()
                    .isEqualTo(new OrdemServicoResponse.RegistroHistorico(AGUARDANDO_APROVACAO, CANCELADA, AGORA));
            verify(pecaInsumoPortOut).liberar(ID_PECA, 2);
            verify(pecaInsumoPortOut).liberar(ID_VELA, 4);
        }

        @Test
        @DisplayName("Deve recusar enviar o orçamento quando não há saldo suficiente para reservar as peças")
        void deveRecusarEnviarSemSaldoParaReserva() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            when(pecaInsumoPortOut.reservar(ID_PECA, 2)).thenReturn(false);

            assertErro(ConflitoException.class, "Saldo insuficiente para reservar a peça Filtro de óleo",
                    () -> useCase.registrarOrcamentoEnviado(ID_OS));

            verify(ordemServicoPortOut, never()).salvar(any());
            verify(pecaInsumoPortOut, never()).salvar(any());
            verify(pecaInsumoPortOut, never()).reservar(ID_VELA, 4);
        }

        @Test
        @DisplayName("Deve recusar aprovar o orçamento quando não existe reserva suficiente")
        void deveRecusarAprovacaoSemReserva() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));
            when(pecaInsumoPortOut.baixar(ID_PECA, 2)).thenReturn(false);

            assertErro(ConflitoException.class, "Não há reserva suficiente para atualizar o estoque da peça Filtro de óleo",
                    () -> useCase.registrarOrcamentoAprovado(ID_OS));

            verify(ordemServicoPortOut, never()).salvar(any());
            verify(pecaInsumoPortOut, never()).baixar(ID_VELA, 4);
        }

        @Test
        @DisplayName("Deve recusar aprovar com a OS fora de AGUARDANDO_APROVACAO, ou de novo, sem gravar")
        void deveRecusarAprovarForaDeAguardando() {
            when(ordemServicoPortOut.buscarPorId(ID_OS))
                    .thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)), Optional.of(osNoStatus(EM_EXECUCAO)));

            assertErro(ConflitoException.class, "Transição inválida: EM_DIAGNOSTICO → EM_EXECUCAO", () -> useCase.registrarOrcamentoAprovado(ID_OS));
            assertErro(ConflitoException.class, "Transição inválida: EM_EXECUCAO → EM_EXECUCAO", () -> useCase.registrarOrcamentoAprovado(ID_OS));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve recusar enviar o orçamento ou rejeitá-lo fora do status esperado, sem gravar")
        void deveRecusarEnviarOuRejeitarForaDeOrdem() {
            when(ordemServicoPortOut.buscarPorId(ID_OS))
                    .thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)), Optional.of(osNoStatus(EM_EXECUCAO)));

            assertErro(ConflitoException.class, "Transição inválida: AGUARDANDO_APROVACAO → AGUARDANDO_APROVACAO",
                    () -> useCase.registrarOrcamentoEnviado(ID_OS));
            assertErro(ConflitoException.class, "Só é possível rejeitar o orçamento com a OS em AGUARDANDO_APROVACAO",
                    () -> useCase.registrarOrcamentoRejeitado(ID_OS));
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve recusar rejeitar o orçamento antes do envio, sem mexer no estoque nem gravar")
        void deveRecusarRejeitarAntesDoEnvio() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));

            assertErro(ConflitoException.class, "Só é possível rejeitar o orçamento com a OS em AGUARDANDO_APROVACAO",
                    () -> useCase.registrarOrcamentoRejeitado(ID_OS));
            verifyNoInteractions(pecaInsumoPortOut);
            verify(ordemServicoPortOut, never()).salvar(any());
        }

        @Test
        @DisplayName("Deve responder 404 quando a OS não existe")
        void deveResponder404ParaOsInexistente() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.empty());

            assertErro(RecursoNaoEncontradoException.class, "Ordem de serviço não encontrada", () -> useCase.registrarOrcamentoEnviado(ID_OS));
            assertErro(RecursoNaoEncontradoException.class, "Ordem de serviço não encontrada", () -> useCase.registrarOrcamentoAprovado(ID_OS));
            assertErro(RecursoNaoEncontradoException.class, "Ordem de serviço não encontrada", () -> useCase.registrarOrcamentoRejeitado(ID_OS));
        }
    }

    @Nested
    @DisplayName("H9 — Tempo médio de execução")
    class TempoMedioDeExecucao {

        @Test
        @DisplayName("Deve calcular a média em minutos e a quantidade de OS consideradas")
        void deveCalcularTempoMedio() {
            when(ordemServicoPortOut.listarTemposExecucao())
                    .thenReturn(List.of(Duration.ofMinutes(60), Duration.ofMinutes(120), Duration.ofMinutes(180)));

            TempoMedioExecucaoResponse response = useCase.consultarTempoMedioExecucao();

            assertThat(response.mediaMinutos()).isEqualTo(120L);
            assertThat(response.quantidadeOrdens()).isEqualTo(3);
        }

        @Test
        @DisplayName("Deve truncar a média para minutos inteiros")
        void deveTruncarParaMinutos() {
            when(ordemServicoPortOut.listarTemposExecucao())
                    .thenReturn(List.of(Duration.ofMinutes(90), Duration.ofMinutes(91)));

            assertThat(useCase.consultarTempoMedioExecucao().mediaMinutos()).isEqualTo(90L);
        }

        @Test
        @DisplayName("Deve responder média nula e quantidade zero quando nenhuma OS foi finalizada")
        void deveResponderVazioSemOsFinalizada() {
            when(ordemServicoPortOut.listarTemposExecucao()).thenReturn(List.of());

            TempoMedioExecucaoResponse response = useCase.consultarTempoMedioExecucao();

            assertThat(response.mediaMinutos()).isNull();
            assertThat(response.quantidadeOrdens()).isZero();
        }
    }

    @Nested
    @DisplayName("Listagem por cliente e métricas da OS")
    class ListagemEMetricas {

        @Test
        @DisplayName("Deve listar as OS do cliente convertidas em resposta")
        void deveListarOrdensDoCliente() {
            when(ordemServicoPortOut.listarOrdens(ID_CLIENTE)).thenReturn(List.of(osNoStatus(ENTREGUE), osNoStatus(RECEBIDA)));

            List<OrdemServicoResponse> ordens = useCase.listar(ID_CLIENTE);

            assertThat(ordens).hasSize(2).extracting(OrdemServicoResponse::status).containsExactly(ENTREGUE, RECEBIDA);
            assertThat(ordens.getFirst().idCliente()).isEqualTo(ID_CLIENTE);
        }

        @Test
        @DisplayName("Deve listar vazio quando o cliente não tem OS")
        void deveListarVazio() {
            when(ordemServicoPortOut.listarOrdens(ID_CLIENTE)).thenReturn(List.of());

            assertThat(useCase.listar(ID_CLIENTE)).isEmpty();
        }

        @Test
        @DisplayName("Deve contar a OS criada")
        void deveContarOsCriada() {
            Cliente cliente = Cliente.reconstituir(ID_CLIENTE, "Maria", new Documento(DOCUMENTO), "maria@email.com",
                    "11999998888", null, true, ABERTURA, ABERTURA);
            when(clientePortOut.buscarAtivoPorDocumento(new Documento(DOCUMENTO))).thenReturn(Optional.of(cliente));
            when(veiculoPortOut.buscarAtivoPorId(ID_VEICULO)).thenReturn(Optional.of(Veiculo.reconstituir(ID_VEICULO,
                    new Placa("BRA2E19"), "Volkswagen", "Gol", 2020, ID_CLIENTE, true, ABERTURA, ABERTURA)));
            retornarOsSalva();

            useCase.criar(new OrdemServicoRequest(DOCUMENTO, ID_VEICULO, List.of(), List.of()));

            assertThat(meterRegistry.get("oficina.os.criadas").counter().count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Deve contar a transição manual de status com os status de origem e destino")
        void deveContarTransicaoManual() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_EXECUCAO)));
            retornarOsSalva();

            useCase.alterarStatus(ID_OS, new OrdemServicoRequest.Status(FINALIZADA));

            assertThat(transicoes(EM_EXECUCAO, FINALIZADA)).isEqualTo(1);
        }

        @Test
        @DisplayName("Não deve contar transição recusada")
        void naoDeveContarTransicaoRecusada() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            var paraFinalizada = new OrdemServicoRequest.Status(FINALIZADA);

            assertThatThrownBy(() -> useCase.alterarStatus(ID_OS, paraFinalizada))
                    .isInstanceOf(ConflitoException.class);

            assertThat(meterRegistry.find("oficina.os.transicoes").counter()).isNull();
        }

        @Test
        @DisplayName("Deve registrar o tempo total da abertura até a entrega")
        void deveRegistrarTempoTotalNaEntrega() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(FINALIZADA)));
            retornarOsSalva();

            useCase.alterarStatus(ID_OS, new OrdemServicoRequest.Status(ENTREGUE));

            Timer tempoTotal = meterRegistry.get("oficina.os.tempo.total").timer();
            assertThat(tempoTotal.count()).isEqualTo(1);
            assertThat(tempoTotal.totalTime(TimeUnit.MINUTES)).isEqualTo(30.0);
            assertThat(transicoes(FINALIZADA, ENTREGUE)).isEqualTo(1);
        }

        @Test
        @DisplayName("Deve registrar o valor do orçamento enviado e as unidades reservadas")
        void deveRegistrarValorDoOrcamentoEnviado() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            when(pecaInsumoPortOut.reservar(ID_PECA, 2)).thenReturn(true);
            when(pecaInsumoPortOut.reservar(ID_VELA, 4)).thenReturn(true);
            retornarOsSalva();

            useCase.registrarOrcamentoEnviado(ID_OS);

            // 1 × R$100 + 2 × R$30 + 4 × R$20
            DistributionSummary valor = meterRegistry.get("oficina.os.orcamento.valor").summary();
            assertThat(valor.count()).isEqualTo(1);
            assertThat(valor.totalAmount()).isEqualTo(240.0);
            assertThat(unidades("reservar")).isEqualTo(6);
            assertThat(transicoes(EM_DIAGNOSTICO, AGUARDANDO_APROVACAO)).isEqualTo(1);
        }

        @Test
        @DisplayName("Deve contar as unidades baixadas na aprovação, sem registrar valor de orçamento")
        void deveContarUnidadesBaixadasNaAprovacao() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));
            when(pecaInsumoPortOut.baixar(ID_PECA, 2)).thenReturn(true);
            when(pecaInsumoPortOut.baixar(ID_VELA, 4)).thenReturn(true);
            retornarOsSalva();

            useCase.registrarOrcamentoAprovado(ID_OS);

            assertThat(unidades("consumir")).isEqualTo(6);
            assertThat(meterRegistry.get("oficina.os.orcamento.valor").summary().count()).isZero();
            assertThat(transicoes(AGUARDANDO_APROVACAO, EM_EXECUCAO)).isEqualTo(1);
        }

        @Test
        @DisplayName("Deve contar a unidades liberadas na rejeição do orçamento")
        void deveContarUnidadesLiberadasNaRejeicao() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(AGUARDANDO_APROVACAO)));
            when(pecaInsumoPortOut.liberar(ID_PECA, 2)).thenReturn(true);
            when(pecaInsumoPortOut.liberar(ID_VELA, 4)).thenReturn(true);
            retornarOsSalva();

            useCase.registrarOrcamentoRejeitado(ID_OS);

            assertThat(unidades("liberar")).isEqualTo(6);
            assertThat(transicoes(AGUARDANDO_APROVACAO, CANCELADA)).isEqualTo(1);
        }

        @Test
        @DisplayName("Deve contar a reserva recusada por falta de saldo, sem transição nem valor de orçamento")
        void deveContarReservaRecusada() {
            when(ordemServicoPortOut.buscarPorId(ID_OS)).thenReturn(Optional.of(osNoStatus(EM_DIAGNOSTICO)));
            when(pecaInsumoPortOut.reservar(ID_PECA, 2)).thenReturn(false);

            assertThatThrownBy(() -> useCase.registrarOrcamentoEnviado(ID_OS)).isInstanceOf(ConflitoException.class);

            Counter recusada = meterRegistry.get("oficina.estoque.movimentacao.recusada").tag("tipo", "reservar").counter();
            assertThat(recusada.count()).isEqualTo(1);
            assertThat(meterRegistry.find("oficina.os.transicoes").counter()).isNull();
            assertThat(meterRegistry.get("oficina.os.orcamento.valor").summary().count()).isZero();
        }
    }

    // ---- montagem de cenários --------------------------------------------------------------------

    private static OrdemServicoRequest.Status mudarStatus(StatusOrdemServico status) {
        return new OrdemServicoRequest.Status(status);
    }

    /**
     * OS gravada no status pedido, com 1 serviço (R$100) e 2 peças: filtro 2 × R$30 e vela 4 × R$20. O
     * histórico só importa para os testes que olham o último registro.
     */
    private static OrdemServico osNoStatus(StatusOrdemServico status) {
        return OrdemServico.reconstituir(ID_OS, ID_CLIENTE, ID_VEICULO, status,
                List.of(ItemServico.reconstituir(1L, ID_SERVICO, "Troca de óleo", ValorMonetario.de("100"), 1)),
                List.of(ItemPeca.reconstituir(2L, ID_PECA, "Filtro de óleo", ValorMonetario.de("30"), 2),
                        ItemPeca.reconstituir(3L, ID_VELA, "Vela", ValorMonetario.de("20"), 4)),
                List.of(new RegistroHistorico(null, RECEBIDA, ABERTURA)));
    }

    /**
     * PUT completo com o veículo e os mesmos itens de {@link #osNoStatus}: só o documento do cliente varia.
     */
    private static OrdemServicoRequest putComItensAtuais(String documento) {
        return new OrdemServicoRequest(documento, ID_VEICULO, List.of(new OrdemServicoRequest.Servico(ID_SERVICO, 1)),
                List.of(new OrdemServicoRequest.Peca(ID_PECA, 2), new OrdemServicoRequest.Peca(ID_VELA, 4)));
    }

    /**
     * OS em execução com o histórico completo até EM_EXECUCAO, para medir o tempo de execução.
     */
    private static OrdemServico osEmExecucaoDesde(OffsetDateTime inicioExecucao) {
        return OrdemServico.reconstituir(ID_OS, ID_CLIENTE, ID_VEICULO, EM_EXECUCAO, List.of(), List.of(), List.of(
                new RegistroHistorico(null, RECEBIDA, ABERTURA.minusHours(2)),
                new RegistroHistorico(RECEBIDA, EM_DIAGNOSTICO, ABERTURA.minusHours(2)),
                new RegistroHistorico(EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, ABERTURA.minusHours(1)),
                new RegistroHistorico(AGUARDANDO_APROVACAO, EM_EXECUCAO, inicioExecucao)));
    }

    private void retornarOsSalva() {
        when(ordemServicoPortOut.salvar(any(OrdemServico.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private void clienteEVeiculoCadastrados() {
        clienteCadastrado();
        when(veiculoPortOut.buscarAtivoPorId(ID_VEICULO)).thenReturn(Optional.of(veiculoDoCliente(ID_CLIENTE)));
    }

    /**
     * O PUT só confere o cliente pelo documento; o veículo é comparado pelo id, sem consultar o cadastro.
     */
    private void clienteCadastrado() {
        when(clientePortOut.buscarAtivoPorDocumento(new Documento(DOCUMENTO))).thenReturn(Optional.of(CLIENTE));
    }

    private static Servico servico(Long id, String nome, String preco) {
        Servico servico = Servico.reconstituir(id, nome, null, null, new BigDecimal(preco), null, null);
        return servico;
    }

    /**
     * Filtro de óleo a R$30, com 3 unidades já reservadas por outras OS além do saldo pedido.
     */
    private static PecaInsumo filtroComSaldo(int saldo) {
        PecaInsumo peca = PecaInsumo.reconstituir(
                ID_PECA,
                "Filtro de óleo",
                null,
                null,
                saldo + 3,
                3,
                new BigDecimal("30"),
                null,
                null);
        return peca;
    }

    private static Veiculo veiculoDoCliente(Long idCliente) {
        return Veiculo.reconstituir(ID_VEICULO, new Placa("BRA2E19"), "Volkswagen", "Gol", 2020, idCliente,
                true, ABERTURA, ABERTURA);
    }

    private static void assertErro(Class<? extends DominioException> tipo, String mensagem, Executable acao) {
        DominioException exception = assertThrows(tipo, acao);
        assertThat(exception.getMessage()).isEqualTo(mensagem);
    }

    private double transicoes(StatusOrdemServico de, StatusOrdemServico para) {
        var contador = meterRegistry.find("oficina.os.transicoes").tags("de", de.name(), "para", para.name()).counter();
        return contador == null ? 0 : contador.count();
    }

    private double unidades(String tipo) {
        var contador = meterRegistry.find("oficina.estoque.movimentacao.unidades").tag("tipo", tipo).counter();
        return contador == null ? 0 : contador.count();
    }
}
