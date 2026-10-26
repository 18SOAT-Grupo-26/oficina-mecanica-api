package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.ItemPeca;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.ItemServico;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.OrdemServico;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.RegistroHistorico;
import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.ValorMonetario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.AGUARDANDO_APROVACAO;
import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.EM_DIAGNOSTICO;
import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.EM_EXECUCAO;
import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.FINALIZADA;
import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.RECEBIDA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class OrdemServicoPersistenceMapperTest {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-10-25T08:00:00-03:00");
    private static final OffsetDateTime T1 = T0.plusMinutes(10);
    private static final OffsetDateTime T2 = T0.plusHours(1);
    private static final OffsetDateTime T3 = T0.plusHours(2);
    private static final Long ID_OS = 99L;
    private static final Long ID_CLIENTE = 10L;
    private static final Long ID_VEICULO = 20L;

    private static final List<RegistroHistorico> HISTORICO_EM_EXECUCAO = List.of(
            new RegistroHistorico(null, RECEBIDA, T0),
            new RegistroHistorico(RECEBIDA, EM_DIAGNOSTICO, T1),
            new RegistroHistorico(EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, T2),
            new RegistroHistorico(AGUARDANDO_APROVACAO, EM_EXECUCAO, T3));

    private final OrdemServicoPersistenceMapper mapper = new OrdemServicoPersistenceMapper();

    @Test
    @DisplayName("Deve converter uma OS nova em entidade, com itens e histórico apontando para ela")
    void deveConverterOsNova() {
        OrdemServico os = OrdemServico.abrir(ID_CLIENTE, ID_VEICULO, T0);
        os.incluirServico(7L, "Troca de óleo", ValorMonetario.de("100"), 1);
        os.incluirPeca(3L, "Filtro de óleo", ValorMonetario.de("30"), 2);

        OrdemServicoEntity entity = mapper.toEntity(os);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getClienteId()).isEqualTo(ID_CLIENTE);
        assertThat(entity.getVeiculoId()).isEqualTo(ID_VEICULO);
        assertThat(entity.getStatus()).isEqualTo(EM_DIAGNOSTICO);
        assertThat(entity.getItensServico()).singleElement().satisfies(item -> {
            assertThat(item.getId()).isNull();
            assertThat(item.getServicoId()).isEqualTo(7L);
            assertThat(item.getDescricao()).isEqualTo("Troca de óleo");
            assertThat(item.getPrecoUnitario()).isEqualByComparingTo("100");
            assertThat(item.getQuantidade()).isEqualTo(1);
            assertThat(item.getOrdemServico()).isSameAs(entity);
        });
        assertThat(entity.getItensPeca()).singleElement().satisfies(item -> {
            assertThat(item.getPecaInsumoId()).isEqualTo(3L);
            assertThat(item.getQuantidade()).isEqualTo(2);
            assertThat(item.getOrdemServico()).isSameAs(entity);
        });
        assertThat(entity.getHistorico())
                .extracting(HistoricoStatusOsEntity::getStatusAntigo, HistoricoStatusOsEntity::getStatusAtualizado,
                        HistoricoStatusOsEntity::getDataCriacao)
                .containsExactly(tuple(null, RECEBIDA, T0), tuple(RECEBIDA, EM_DIAGNOSTICO, T0));
        assertThat(entity.getHistorico()).allSatisfy(registro -> assertThat(registro.getOrdemServico()).isSameAs(entity));
    }

    @Test
    @DisplayName("Deve reconstituir a OS a partir da entidade, com itens e histórico")
    void deveConverterParaDominio() {
        OrdemServicoEntity entity = entidadeEmExecucao();

        OrdemServico os = mapper.toDomain(entity);

        assertThat(os.getId()).isEqualTo(ID_OS);
        assertThat(os.getIdCliente()).isEqualTo(ID_CLIENTE);
        assertThat(os.getIdVeiculo()).isEqualTo(ID_VEICULO);
        assertThat(os.getStatus()).isEqualTo(EM_EXECUCAO);
        assertThat(os.getItensServico()).singleElement().usingRecursiveComparison()
                .isEqualTo(ItemServico.reconstituir(1L, 7L, "Troca de óleo", ValorMonetario.de("100"), 1));
        assertThat(os.getItensPeca()).singleElement().usingRecursiveComparison()
                .isEqualTo(ItemPeca.reconstituir(2L, 3L, "Filtro de óleo", ValorMonetario.de("30"), 2));
        assertThat(os.getHistorico()).containsExactlyElementsOf(HISTORICO_EM_EXECUCAO);
    }

    @Test
    @DisplayName("Deve sincronizar a entidade existente: remove o que saiu, inclui o novo e só acrescenta histórico")
    void deveSincronizarEntidadeExistente() {
        OrdemServicoEntity entity = entidadeEmExecucao();
        HistoricoStatusOsEntity primeiroRegistro = entity.getHistorico().getFirst();
        OrdemServico os = OrdemServico.reconstituir(ID_OS, ID_CLIENTE, ID_VEICULO, EM_EXECUCAO,
                List.of(ItemServico.reconstituir(1L, 7L, "Troca de óleo", ValorMonetario.de("100"), 1),
                        ItemServico.novo(8L, "Alinhamento", ValorMonetario.de("80"), 1)),
                List.of(),
                HISTORICO_EM_EXECUCAO);
        os.finalizar(T3.plusHours(2));

        mapper.copiar(os, entity);

        assertThat(entity.getItensServico()).extracting(ItemServicoOsEntity::getId, ItemServicoOsEntity::getServicoId)
                .containsExactly(tuple(1L, 7L), tuple(null, 8L));
        assertThat(entity.getItensPeca()).isEmpty();
        assertThat(entity.getHistorico()).hasSize(5);
        assertThat(entity.getHistorico().getFirst()).isSameAs(primeiroRegistro);
        assertThat(entity.getHistorico().getLast().getIdHistorico()).isNull();
        assertThat(entity.getHistorico().getLast().getStatusAntigo()).isEqualTo(EM_EXECUCAO);
        assertThat(entity.getHistorico().getLast().getStatusAtualizado()).isEqualTo(FINALIZADA);
        assertThat(entity.getStatus()).isEqualTo(FINALIZADA);
    }

    /** OS em execução, já gravada: 1 serviço (R$100), 1 peça (2 × R$30) e quatro registros no histórico. */
    static OrdemServicoEntity entidadeEmExecucao() {
        OrdemServicoEntity entity = new OrdemServicoEntity();
        entity.setId(ID_OS);
        entity.setClienteId(ID_CLIENTE);
        entity.setVeiculoId(ID_VEICULO);
        entity.setStatus(EM_EXECUCAO);

        ItemServicoOsEntity servico = new ItemServicoOsEntity();
        servico.setId(1L);
        servico.setOrdemServico(entity);
        servico.setServicoId(7L);
        servico.setDescricao("Troca de óleo");
        servico.setPrecoUnitario(new BigDecimal("100.00"));
        servico.setQuantidade(1);
        entity.getItensServico().add(servico);

        ItemPecaOsEntity peca = new ItemPecaOsEntity();
        peca.setId(2L);
        peca.setOrdemServico(entity);
        peca.setPecaInsumoId(3L);
        peca.setDescricao("Filtro de óleo");
        peca.setPrecoUnitario(new BigDecimal("30.00"));
        peca.setQuantidade(2);
        entity.getItensPeca().add(peca);

        long idRegistro = 1;
        for (RegistroHistorico registro : HISTORICO_EM_EXECUCAO) {
            HistoricoStatusOsEntity historico = new HistoricoStatusOsEntity();
            historico.setIdHistorico(idRegistro++);
            historico.setOrdemServico(entity);
            historico.setStatusAntigo(registro.statusAntigo());
            historico.setStatusAtualizado(registro.statusAtualizado());
            historico.setDataCriacao(registro.dataHora());
            entity.getHistorico().add(historico);
        }
        return entity;
    }
}
