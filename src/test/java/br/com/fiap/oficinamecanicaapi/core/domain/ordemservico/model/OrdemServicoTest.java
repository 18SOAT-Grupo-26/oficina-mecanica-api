package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdemServicoTest {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-10-24T08:00:00-03:00");
    private static final OffsetDateTime T1 = T0.plusMinutes(10);
    private static final OffsetDateTime T2 = T0.plusMinutes(40);
    private static final OffsetDateTime T3 = T0.plusHours(2);
    private static final OffsetDateTime T4 = T0.plusHours(3);
    private static final OffsetDateTime T5 = T0.plusHours(5).plusMinutes(30);
    private static final OffsetDateTime T6 = T0.plusHours(8);
    private static final Long ID_CLIENTE = 10L;
    private static final Long ID_VEICULO = 20L;
    private static final Long ID_SERVICO = 7L;
    private static final Long ID_PECA = 3L;

    @Nested
    @DisplayName("H2 — Abertura e histórico de status")
    class AberturaEHistorico {

        @Test
        @DisplayName("Deve abrir a OS com cliente e veículo e levá-la sozinha para EM_DIAGNOSTICO, registrando os dois status")
        void deveAbrirEIrParaDiagnostico() {
            OrdemServico os = OrdemServico.abrir(ID_CLIENTE, ID_VEICULO, T0);

            assertThat(os.getId()).isNull();
            assertThat(os.getIdCliente()).isEqualTo(ID_CLIENTE);
            assertThat(os.getIdVeiculo()).isEqualTo(ID_VEICULO);
            assertThat(os.getStatus()).isEqualTo(EM_DIAGNOSTICO);
            assertThat(os.getHistorico()).containsExactly(
                    new RegistroHistorico(null, RECEBIDA, T0),
                    new RegistroHistorico(RECEBIDA, EM_DIAGNOSTICO, T0));
        }

        @Test
        @DisplayName("Deve recusar abrir a OS sem cliente ou sem veículo com erro 400")
        void deveExigirClienteEVeiculoNaAbertura() {
            assertErro(400, "O cliente da OS é obrigatório", () -> OrdemServico.abrir(null, ID_VEICULO, T0));
            assertErro(400, "O veículo da OS é obrigatório", () -> OrdemServico.abrir(ID_CLIENTE, null, T0));
        }

        @Test
        @DisplayName("Deve acrescentar um registro a cada transição válida, mantendo os anteriores")
        void deveAcrescentarRegistroNaTransicao() {
            OrdemServico os = osEmDiagnosticoComItens();

            os.aguardarAprovacao(T2);

            assertThat(os.getHistorico()).containsExactly(
                    new RegistroHistorico(null, RECEBIDA, T0),
                    new RegistroHistorico(RECEBIDA, EM_DIAGNOSTICO, T0),
                    new RegistroHistorico(EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, T2));
        }

        @Test
        @DisplayName("Deve recusar transição inválida com erro 409, sem alterar status nem histórico")
        void deveRecusarTransicaoInvalida() {
            OrdemServico os = osEmDiagnostico();

            assertErro(409, "Transição inválida: EM_DIAGNOSTICO → FINALIZADA", () -> os.finalizar(T1));
            assertThat(os.getStatus()).isEqualTo(EM_DIAGNOSTICO);
            assertThat(os.getHistorico()).hasSize(2);
        }

        @Test
        @DisplayName("Deve expor o histórico somente para leitura")
        void deveExporHistoricoImutavel() {
            List<RegistroHistorico> historico = osEmDiagnostico().getHistorico();

            assertThrows(UnsupportedOperationException.class,
                    () -> historico.add(new RegistroHistorico(FINALIZADA, ENTREGUE, T1)));
        }

        @Test
        @DisplayName("Deve exigir data/hora para abrir e para mudar de status")
        void deveExigirDataHora() {
            assertErro(400, "A data/hora da operação é obrigatória", () -> OrdemServico.abrir(ID_CLIENTE, ID_VEICULO, null));
            OrdemServico os = osEmDiagnostico();
            assertErro(400, "A data/hora da operação é obrigatória", () -> os.aguardarAprovacao(null));
        }

        @Test
        @DisplayName("Deve reconstituir uma OS persistida com todos os dados")
        void deveReconstituir() {
            ItemServico servico = ItemServico.reconstituir(1L, ID_SERVICO, "Troca de óleo", ValorMonetario.de("100"), 1);
            ItemPeca peca = ItemPeca.reconstituir(2L, ID_PECA, "Filtro", ValorMonetario.de("30"), 2);
            List<RegistroHistorico> historico = List.of(
                    new RegistroHistorico(null, RECEBIDA, T0),
                    new RegistroHistorico(RECEBIDA, EM_DIAGNOSTICO, T1),
                    new RegistroHistorico(EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, T2),
                    new RegistroHistorico(AGUARDANDO_APROVACAO, EM_EXECUCAO, T3));

            OrdemServico os = OrdemServico.reconstituir(99L, ID_CLIENTE, ID_VEICULO, EM_EXECUCAO,
                    List.of(servico), List.of(peca), historico);

            assertThat(os.getId()).isEqualTo(99L);
            assertThat(os.getIdCliente()).isEqualTo(ID_CLIENTE);
            assertThat(os.getIdVeiculo()).isEqualTo(ID_VEICULO);
            assertThat(os.getStatus()).isEqualTo(EM_EXECUCAO);
            assertThat(os.getItensServico()).containsExactly(servico);
            assertThat(os.getItensPeca()).containsExactly(peca);
            assertThat(os.getHistorico()).isEqualTo(historico);
        }
    }

    @Nested
    @DisplayName("H4 — Itens da OS")
    class Itens {

        @Test
        @DisplayName("Deve incluir serviço e peça com o preço congelado e o subtotal de cada um")
        void deveIncluirItens() {
            OrdemServico os = osEmDiagnostico();

            os.incluirServico(ID_SERVICO, "Troca de óleo", ValorMonetario.de("100"), 1);
            os.incluirPeca(ID_PECA, "Filtro de óleo", ValorMonetario.de("30"), 2);

            assertThat(os.getItensServico()).singleElement().satisfies(item -> {
                assertThat(item.getIdServico()).isEqualTo(ID_SERVICO);
                assertThat(item.getPrecoUnitario()).isEqualTo(ValorMonetario.de("100"));
                assertThat(item.getSubtotal()).isEqualTo(ValorMonetario.de("100"));
            });
            assertThat(os.getItensPeca()).singleElement().satisfies(item -> {
                assertThat(item.getIdPeca()).isEqualTo(ID_PECA);
                assertThat(item.getSubtotal()).isEqualTo(ValorMonetario.de("60"));
            });
        }

        @Test
        @DisplayName("Deve remover serviço e peça")
        void deveRemoverItens() {
            OrdemServico os = osEmDiagnosticoComItens();

            os.removerPeca(ID_PECA);
            os.removerServico(ID_SERVICO);

            assertThat(os.getItensServico()).isEmpty();
            assertThat(os.getItensPeca()).isEmpty();
        }

        @Test
        @DisplayName("Deve recusar remover item que não está na OS com erro 404")
        void deveRecusarRemoverItemInexistente() {
            OrdemServico os = osEmDiagnosticoComItens();

            assertErro(404, "Serviço não encontrado na OS", () -> os.removerServico(999L));
            assertErro(404, "Peça não encontrada na OS", () -> os.removerPeca(999L));
        }

        @Test
        @DisplayName("Deve recusar incluir o mesmo serviço ou peça duas vezes com erro 409")
        void deveRecusarItemDuplicado() {
            OrdemServico os = osEmDiagnosticoComItens();

            assertErro(409, "Serviço já incluído na OS", () -> os.incluirServico(ID_SERVICO, "Troca", ValorMonetario.de("1"), 1));
            assertErro(409, "Peça já incluída na OS", () -> os.incluirPeca(ID_PECA, "Filtro", ValorMonetario.de("1"), 1));
        }

        @Test
        @DisplayName("Deve validar os dados do item ao incluir")
        void deveValidarItemAoIncluir() {
            OrdemServico os = osEmDiagnostico();

            assertErro(400, "A quantidade do item deve ser maior que zero",
                    () -> os.incluirPeca(ID_PECA, "Filtro", ValorMonetario.de("30"), 0));
            assertThat(os.getItensPeca()).isEmpty();
        }

        @Test
        @DisplayName("Deve recusar alterar itens a partir de AGUARDANDO_APROVACAO com erro 409")
        void deveRecusarAlterarItensDepoisDoEnvio() {
            OrdemServico os = osAguardandoAprovacao();
            String mensagem = "Itens só podem ser alterados com a OS em RECEBIDA ou EM_DIAGNOSTICO";

            assertErro(409, mensagem, () -> os.incluirServico(8L, "Alinhamento", ValorMonetario.de("80"), 1));
            assertErro(409, mensagem, () -> os.incluirPeca(4L, "Vela", ValorMonetario.de("20"), 1));
            assertErro(409, mensagem, () -> os.removerServico(ID_SERVICO));
            assertErro(409, mensagem, () -> os.removerPeca(ID_PECA));
            assertThat(os.getItensServico()).hasSize(1);
            assertThat(os.getItensPeca()).hasSize(1);
        }

        @Test
        @DisplayName("Deve expor as listas de itens somente para leitura")
        void deveExporItensImutaveis() {
            OrdemServico os = osEmDiagnosticoComItens();

            assertThrows(UnsupportedOperationException.class, () -> os.getItensServico().clear());
            assertThrows(UnsupportedOperationException.class, () -> os.getItensPeca().clear());
        }
    }

    @Nested
    @DisplayName("H5 — Aprovação, execução e entrega")
    class AprovacaoExecucaoEEntrega {

        @Test
        @DisplayName("Deve aguardar aprovação quando o orçamento é enviado")
        void deveAguardarAprovacao() {
            OrdemServico os = osEmDiagnosticoComItens();

            os.aguardarAprovacao(T2);

            assertThat(os.getStatus()).isEqualTo(AGUARDANDO_APROVACAO);
        }

        @Test
        @DisplayName("Deve recusar aguardar aprovação sem itens com erro 409")
        void deveRecusarAguardarAprovacaoSemItens() {
            OrdemServico os = osEmDiagnostico();

            assertErro(409, "A OS precisa de ao menos um item para gerar orçamento", () -> os.aguardarAprovacao(T2));
            assertThat(os.getStatus()).isEqualTo(EM_DIAGNOSTICO);
        }

        @Test
        @DisplayName("Deve iniciar a execução quando o orçamento é aprovado")
        void deveIniciarExecucao() {
            OrdemServico os = osAguardandoAprovacao();

            os.iniciarExecucao(T3);

            assertThat(os.getStatus()).isEqualTo(EM_EXECUCAO);
            assertThat(os.getHistorico()).last().isEqualTo(new RegistroHistorico(AGUARDANDO_APROVACAO, EM_EXECUCAO, T3));
        }

        @Test
        @DisplayName("Deve recusar iniciar a execução fora de AGUARDANDO_APROVACAO ou duas vezes, com erro 409")
        void deveRecusarIniciarExecucaoForaDeAguardando() {
            OrdemServico emDiagnostico = osEmDiagnostico();
            OrdemServico emExecucao = osAguardandoAprovacao();
            emExecucao.iniciarExecucao(T3);

            assertErro(409, "Transição inválida: EM_DIAGNOSTICO → EM_EXECUCAO", () -> emDiagnostico.iniciarExecucao(T3));
            assertErro(409, "Transição inválida: EM_EXECUCAO → EM_EXECUCAO", () -> emExecucao.iniciarExecucao(T4));
            assertThat(emExecucao.getHistorico()).hasSize(4);
        }

        @Test
        @DisplayName("Deve recusar comandos fora do status esperado com erro 409")
        void deveRecusarComandosForaDeOrdem() {
            OrdemServico os = osEmDiagnostico();

            assertErro(409, "Transição inválida: EM_DIAGNOSTICO → FINALIZADA", () -> os.finalizar(T3));
            assertErro(409, "Transição inválida: EM_DIAGNOSTICO → ENTREGUE", () -> os.entregar(T3));
            assertThat(os.getStatus()).isEqualTo(EM_DIAGNOSTICO);
        }

        @Test
        @DisplayName("Deve percorrer o fluxo feliz com os seis status em ordem no histórico")
        void devePercorrerFluxoFeliz() {
            OrdemServico os = osFinalizada();

            os.entregar(T6);

            assertThat(os.getStatus()).isEqualTo(ENTREGUE);
            assertThat(os.getHistorico()).containsExactly(
                    new RegistroHistorico(null, RECEBIDA, T0),
                    new RegistroHistorico(RECEBIDA, EM_DIAGNOSTICO, T0),
                    new RegistroHistorico(EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, T2),
                    new RegistroHistorico(AGUARDANDO_APROVACAO, EM_EXECUCAO, T4),
                    new RegistroHistorico(EM_EXECUCAO, FINALIZADA, T5),
                    new RegistroHistorico(FINALIZADA, ENTREGUE, T6));
        }
    }

    @Nested
    @DisplayName("Edição da OS")
    class Edicao {

        @Test
        @DisplayName("Deve aceitar edição antes de EM_EXECUCAO")
        void deveAceitarEdicaoAntesDeEmExecucao() {
            for (OrdemServico os : List.of(osRecebidaReconstituida(), osEmDiagnostico(), osAguardandoAprovacao())) {
                os.exigirEditavel();
            }
        }

        @Test
        @DisplayName("Deve recusar edição a partir de EM_EXECUCAO e com a OS cancelada, com erro 409")
        void deveRecusarEdicaoAPartirDeEmExecucao() {
            OrdemServico emExecucao = osAguardandoAprovacao();
            emExecucao.iniciarExecucao(T3);
            OrdemServico entregue = osFinalizada();
            entregue.entregar(T6);
            OrdemServico cancelada = osEmDiagnostico();
            cancelada.cancelar(T1);

            assertErro(409, "A OS não pode ser editada com status EM_EXECUCAO", emExecucao::exigirEditavel);
            assertErro(409, "A OS não pode ser editada com status FINALIZADA", osFinalizada()::exigirEditavel);
            assertErro(409, "A OS não pode ser editada com status ENTREGUE", entregue::exigirEditavel);
            assertErro(409, "A OS não pode ser editada com status CANCELADA", cancelada::exigirEditavel);
        }
    }

    @Nested
    @DisplayName("Cancelamento da OS")
    class Cancelamento {

        @Test
        @DisplayName("Deve cancelar em RECEBIDA, EM_DIAGNOSTICO e AGUARDANDO_APROVACAO, gravando o histórico")
        void deveCancelarAteAguardandoAprovacao() {
            for (OrdemServico os : List.of(osRecebidaReconstituida(), osEmDiagnostico(), osAguardandoAprovacao())) {
                StatusOrdemServico anterior = os.getStatus();

                os.cancelar(T3);

                assertThat(os.getStatus()).isEqualTo(CANCELADA);
                assertThat(os.getHistorico()).last().isEqualTo(new RegistroHistorico(anterior, CANCELADA, T3));
            }
        }

        @Test
        @DisplayName("Deve recusar cancelar a partir de EM_EXECUCAO ou de novo, com erro 409, sem mudar o histórico")
        void deveRecusarCancelarAPartirDeEmExecucao() {
            OrdemServico emExecucao = osAguardandoAprovacao();
            emExecucao.iniciarExecucao(T3);
            OrdemServico cancelada = osEmDiagnostico();
            cancelada.cancelar(T1);

            assertErro(409, "Transição inválida: EM_EXECUCAO → CANCELADA", () -> emExecucao.cancelar(T4));
            assertErro(409, "Transição inválida: FINALIZADA → CANCELADA", () -> osFinalizada().cancelar(T6));
            assertErro(409, "Transição inválida: CANCELADA → CANCELADA", () -> cancelada.cancelar(T2));
            assertThat(emExecucao.getStatus()).isEqualTo(EM_EXECUCAO);
            assertThat(cancelada.getHistorico()).hasSize(3);
        }

        @Test
        @DisplayName("Deve exigir data/hora para cancelar")
        void deveExigirDataHoraParaCancelar() {
            assertErro(400, "A data/hora da operação é obrigatória", () -> osEmDiagnostico().cancelar(null));
        }
    }

    @Nested
    @DisplayName("H6 — Tempo de execução")
    class TempoDeExecucao {

        @Test
        @DisplayName("Deve medir o tempo de EM_EXECUCAO até FINALIZADA")
        void deveMedirTempoDeExecucao() {
            assertThat(osFinalizada().getTempoExecucao()).contains(Duration.ofHours(2).plusMinutes(30));
        }

        @Test
        @DisplayName("Deve manter o tempo de execução depois da entrega")
        void deveIgnorarEntrega() {
            OrdemServico os = osFinalizada();
            os.entregar(T6);

            assertThat(os.getTempoExecucao()).contains(Duration.ofHours(2).plusMinutes(30));
        }

        @Test
        @DisplayName("Deve não ter tempo de execução antes de finalizar ou quando cancelada")
        void deveNaoTerTempoSemFinalizar() {
            OrdemServico emExecucao = osAguardandoAprovacao();
            emExecucao.iniciarExecucao(T4);
            OrdemServico cancelada = osAguardandoAprovacao();
            cancelada.cancelar(T3);

            assertThat(emExecucao.getTempoExecucao()).isEmpty();
            assertThat(cancelada.getTempoExecucao()).isEmpty();
        }
    }

    // ---- montagem de cenários --------------------------------------------------------------------

    /** A abertura já leva a OS para EM_DIAGNOSTICO. */
    private static OrdemServico osEmDiagnostico() {
        return OrdemServico.abrir(ID_CLIENTE, ID_VEICULO, T0);
    }

    private static OrdemServico osEmDiagnosticoComItens() {
        OrdemServico os = osEmDiagnostico();
        os.incluirServico(ID_SERVICO, "Troca de óleo", ValorMonetario.de("100"), 1);
        os.incluirPeca(ID_PECA, "Filtro de óleo", ValorMonetario.de("30"), 2);
        return os;
    }

    /** Nenhuma OS nova para em RECEBIDA; esta simula uma gravada assim, para as regras que ainda a aceitam. */
    private static OrdemServico osRecebidaReconstituida() {
        return OrdemServico.reconstituir(99L, ID_CLIENTE, ID_VEICULO, RECEBIDA, List.of(), List.of(),
                List.of(new RegistroHistorico(null, RECEBIDA, T0)));
    }

    private static OrdemServico osAguardandoAprovacao() {
        OrdemServico os = osEmDiagnosticoComItens();
        os.aguardarAprovacao(T2);
        return os;
    }

    private static OrdemServico osFinalizada() {
        OrdemServico os = osAguardandoAprovacao();
        os.iniciarExecucao(T4);
        os.finalizar(T5);
        return os;
    }

    private static void assertErro(int codigo, String mensagem, Executable acao) {
        ApplicationException exception = assertThrows(ApplicationException.class, acao);
        assertThat(exception.getErrorCode()).isEqualTo(codigo);
        assertThat(exception.getMessage()).isEqualTo(mensagem);
    }
}
