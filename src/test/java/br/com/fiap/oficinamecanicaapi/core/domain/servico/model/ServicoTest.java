package br.com.fiap.oficinamecanicaapi.core.domain.servico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicoTest {

    private static final String NOME = "Troca de Óleo";
    private static final String DESCRICAO = "Óleo sintético 5W30";
    private static final BigDecimal PRECO = new BigDecimal("80");
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-21T10:00:00-03:00");

    @Test
    @DisplayName("Deve criar o serviço com nome aparado e normalizado e preço com 2 casas")
    void deveCriar() {
        Servico servico = Servico.novo("  Troca de Óleo ", DESCRICAO, PRECO);

        assertThat(servico.getId()).isNull();
        assertThat(servico.getNome()).isEqualTo(NOME);
        assertThat(servico.getNomeNormalizado()).isEqualTo("troca de oleo");
        assertThat(servico.getDescricao()).isEqualTo(DESCRICAO);
        assertThat(servico.getPreco()).isEqualTo(new BigDecimal("80.00"));
        assertThat(servico.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Desativar deve só marcar o serviço como inativo, mantendo os dados")
    void deveDesativar() {
        Servico servico = Servico.reconstituir(1L, NOME, "troca de oleo", DESCRICAO, new BigDecimal("80.00"),
                true, CRIADO_EM, CRIADO_EM);

        servico.desativar();

        assertThat(servico.isAtivo()).isFalse();
        assertThat(servico.getId()).isEqualTo(1L);
        assertThat(servico.getNome()).isEqualTo(NOME);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("Deve rejeitar com 400 o serviço sem nome")
    void deveRejeitarSemNome(String nome) {
        assertThatThrownBy(() -> Servico.novo(nome, DESCRICAO, PRECO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("O campo nome é obrigatório");
    }

    @Test
    @DisplayName("Deve rejeitar com 400 o nome com menos de 3 caracteres")
    void deveRejeitarNomeCurto() {
        assertThatThrownBy(() -> Servico.novo(" ab ", DESCRICAO, PRECO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("O nome deve ter no mínimo 3 caracteres");
    }

    @Test
    @DisplayName("Deve rejeitar com 400 o serviço sem preço")
    void deveRejeitarSemPreco() {
        assertThatThrownBy(() -> Servico.novo(NOME, DESCRICAO, null))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("O campo preco é obrigatório");
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "10.001"})
    @DisplayName("Deve rejeitar com 400 o preço zero, negativo ou com mais de 2 casas")
    void deveRejeitarPrecoInvalido(String preco) {
        BigDecimal valor = new BigDecimal(preco);

        assertThatThrownBy(() -> Servico.novo(NOME, DESCRICAO, valor))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("Preço inválido");
    }

    @Test
    @DisplayName("Deve rejeitar com 400 a descrição com mais de 500 caracteres")
    void deveRejeitarDescricaoLonga() {
        String longa = "a".repeat(501);

        assertThatThrownBy(() -> Servico.novo(NOME, longa, PRECO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("A descrição deve ter no máximo 500 caracteres");
    }

    @Test
    @DisplayName("Deve atualizar nome, descrição e preço mantendo id e data de criação")
    void deveAtualizar() {
        Servico servico = Servico.reconstituir(1L, NOME, "troca de oleo", DESCRICAO, new BigDecimal("80.00"),
                true, CRIADO_EM, CRIADO_EM);

        servico.atualizar("Alinhamento", null, new BigDecimal("120.5"));

        assertThat(servico.getId()).isEqualTo(1L);
        assertThat(servico.getNome()).isEqualTo("Alinhamento");
        assertThat(servico.getNomeNormalizado()).isEqualTo("alinhamento");
        assertThat(servico.getDescricao()).isNull();
        assertThat(servico.getPreco()).isEqualTo(new BigDecimal("120.50"));
        assertThat(servico.getCriadoEm()).isEqualTo(CRIADO_EM);
    }

    @Test
    @DisplayName("Atualização inválida não deve alterar nenhum campo do serviço")
    void naoDeveAlterarComAtualizacaoInvalida() {
        Servico servico = Servico.reconstituir(1L, NOME, "troca de oleo", DESCRICAO, new BigDecimal("80.00"),
                true, CRIADO_EM, CRIADO_EM);
        BigDecimal precoInvalido = new BigDecimal("-5");

        assertThatThrownBy(() -> servico.atualizar("Outro nome", DESCRICAO, precoInvalido))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(servico.getNome()).isEqualTo(NOME);
        assertThat(servico.getPreco()).isEqualTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("Reconstituir deve aceitar o dado gravado sem validar de novo")
    void deveReconstituirSemValidar() {
        Servico antigo = Servico.reconstituir(2L, "ab", "ab", null, new BigDecimal("1.00"), true, CRIADO_EM,
                null);

        assertThat(antigo.getNome()).isEqualTo("ab");
        assertThat(antigo.getId()).isEqualTo(2L);
    }
}
