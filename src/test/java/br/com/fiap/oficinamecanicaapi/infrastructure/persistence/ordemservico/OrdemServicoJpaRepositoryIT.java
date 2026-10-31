package br.com.fiap.oficinamecanicaapi.infrastructure.persistence.ordemservico;

import br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico;
import br.com.fiap.oficinamecanicaapi.integracao.IntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consultas JPQL do repositório de OS contra o schema real das migrations.
 */
@Transactional
class OrdemServicoJpaRepositoryIT extends IntegracaoBase {

    @Autowired
    private OrdemServicoJpaRepository repository;

    private Long clienteA;
    private Long clienteB;
    private Long veiculoA;
    private Long veiculoB;

    @BeforeEach
    void dados() {
        clienteA = cliente("Ana", "52998224725");
        clienteB = cliente("Bruno", "11144477735");
        veiculoA = veiculo("BRA2E19", clienteA);
        veiculoB = veiculo("ABC1234", clienteB);
    }

    @Test
    @DisplayName("listarOrdensByClientId deve trazer só as OS do cliente, da mais nova para a mais antiga")
    void listarPorCliente() {
        Long antiga = os(clienteA, veiculoA, "EM_DIAGNOSTICO", "now() - interval '2 days'");
        Long nova = os(clienteA, veiculoA, "RECEBIDA", "now()");
        os(clienteB, veiculoB, "RECEBIDA", "now()");

        assertThat(repository.listarOrdensByClientId(clienteA))
                .extracting(OrdemServicoEntity::getId)
                .containsExactly(nova, antiga);
        assertThat(repository.listarOrdensByClientId(9999L)).isEmpty();
    }

    @Test
    @DisplayName("listar deve aplicar só os filtros informados e paginar da mais nova para a mais antiga")
    void listarComFiltros() {
        Long antigaA = os(clienteA, veiculoA, "ENTREGUE", "now() - interval '2 days'");
        Long novaA = os(clienteA, veiculoA, "EM_DIAGNOSTICO", "now()");
        Long b = os(clienteB, veiculoB, "ENTREGUE", "now() - interval '1 day'");
        PageRequest primeira = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        assertThat(repository.listar(null, null, primeira))
                .extracting(OrdemServicoEntity::getId)
                .containsExactly(novaA, b, antigaA);
        assertThat(repository.listar(clienteA, null, primeira))
                .extracting(OrdemServicoEntity::getId)
                .containsExactly(novaA, antigaA);
        assertThat(repository.listar(null, StatusOrdemServico.ENTREGUE, primeira))
                .extracting(OrdemServicoEntity::getId)
                .containsExactly(b, antigaA);
        assertThat(repository.listar(clienteA, StatusOrdemServico.ENTREGUE, primeira))
                .extracting(OrdemServicoEntity::getId)
                .containsExactly(antigaA);

        var segunda = repository.listar(null, null, PageRequest.of(1, 2, primeira.getSort()));
        assertThat(segunda.getContent()).extracting(OrdemServicoEntity::getId).containsExactly(antigaA);
        assertThat(segunda.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("countByStatus deve contar por status")
    void contarPorStatus() {
        os(clienteA, veiculoA, "EM_EXECUCAO", "now()");
        os(clienteA, veiculoA, "EM_EXECUCAO", "now()");
        os(clienteB, veiculoB, "ENTREGUE", "now()");

        assertThat(repository.countByStatus(StatusOrdemServico.EM_EXECUCAO)).isEqualTo(2);
        assertThat(repository.countByStatus(StatusOrdemServico.ENTREGUE)).isEqualTo(1);
        assertThat(repository.countByStatus(StatusOrdemServico.CANCELADA)).isZero();
    }

    @Test
    @DisplayName("listarPeriodosExecucao deve juntar EM_EXECUCAO e FINALIZADA da mesma OS e ignorar as que não finalizaram")
    void periodosExecucao() {
        Long finalizada = os(clienteA, veiculoA, "FINALIZADA", "now()");
        historico(finalizada, "AGUARDANDO_APROVACAO", "EM_EXECUCAO", "now() - interval '90 minutes'");
        historico(finalizada, "EM_EXECUCAO", "FINALIZADA", "now() - interval '30 minutes'");
        Long emExecucao = os(clienteB, veiculoB, "EM_EXECUCAO", "now()");
        historico(emExecucao, "AGUARDANDO_APROVACAO", "EM_EXECUCAO", "now()");

        var periodos = repository.listarPeriodosExecucao();

        assertThat(periodos).hasSize(1);
        assertThat(Duration.between(periodos.getFirst().inicio(), periodos.getFirst().fim()))
                .isEqualTo(Duration.ofMinutes(60));
    }

    private Long cliente(String nome, String cpf) {
        return jdbcTemplate.queryForObject("""
                insert into clientes (nome, documento, tipo_documento, email, telefone, ativo, created_at, updated_at)
                values (?, ?, 'CPF', 'cliente@oficina.com.br', '11999999999', true, now(), now()) returning id
                """, Long.class, nome, cpf);
    }

    private Long veiculo(String placa, Long clienteId) {
        return jdbcTemplate.queryForObject("""
                insert into veiculos (placa, marca, modelo, ano, cliente_id, ativo, created_at, updated_at)
                values (?, 'Fiat', 'Uno', 2020, ?, true, now(), now()) returning id
                """, Long.class, placa, clienteId);
    }

    private Long os(Long clienteId, Long veiculoId, String status, String criadoEm) {
        return jdbcTemplate.queryForObject(
                "insert into ordem_servico (cliente_id, veiculo_id, status, created_at, updated_at) "
                        + "values (?, ?, ?, " + criadoEm + ", now()) returning id",
                Long.class, clienteId, veiculoId, status);
    }

    private void historico(Long os, String antigo, String atual, String quando) {
        jdbcTemplate.update(
                "insert into historico_status_os (id_os, status_antigo, status_atualizado, data_criacao) "
                        + "values (?, ?, ?, " + quando + ")",
                os, antigo, atual);
    }
}
