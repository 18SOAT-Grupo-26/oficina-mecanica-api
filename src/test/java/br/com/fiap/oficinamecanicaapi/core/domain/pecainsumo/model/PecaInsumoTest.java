package br.com.fiap.oficinamecanicaapi.core.domain.pecainsumo.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PecaInsumoTest {

    private static final String NOME = "Filtro de Óleo";
    private static final BigDecimal PRECO = new BigDecimal("30");
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-10-21T10:00:00-03:00");

    @Test
    @DisplayName("Deve criar a peça com nome normalizado, preço com 2 casas e reservado zero quando não informado")
    void deveCriar() {
        PecaInsumo peca = PecaInsumo.novo(" Filtro de Óleo ", null, 10, null, PRECO);

        assertThat(peca.getId()).isNull();
        assertThat(peca.getNome()).isEqualTo(NOME);
        assertThat(peca.getNomeNormalizado()).isEqualTo("filtro de oleo");
        assertThat(peca.getQuantidade()).isEqualTo(10);
        assertThat(peca.getReservado()).isZero();
        assertThat(peca.getPreco()).isEqualTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("Deve rejeitar com 400 a peça sem quantidade")
    void deveRejeitarSemQuantidade() {
        assertThatThrownBy(() -> PecaInsumo.novo(NOME, null, null, 0, PRECO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("O campo quantidade é obrigatório");
    }

    @Test
    @DisplayName("Deve rejeitar com 400 a quantidade negativa")
    void deveRejeitarQuantidadeNegativa() {
        assertThatThrownBy(() -> PecaInsumo.novo(NOME, null, -1, 0, PRECO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("Quantidade inválida");
    }

    @ParameterizedTest(name = "quantidade {0}, reservado {1}")
    @CsvSource({"5, -1", "5, 6"})
    @DisplayName("Deve rejeitar com 400 o reservado negativo ou maior que a quantidade")
    void deveRejeitarReservadoInvalido(int quantidade, int reservado) {
        assertThatThrownBy(() -> PecaInsumo.novo(NOME, null, quantidade, reservado, PRECO))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("Quantidade reservada inválida. Deve ser entre 0 e a quantidade em estoque");
    }

    @Test
    @DisplayName("Deve aceitar o reservado igual à quantidade (tudo reservado)")
    void deveAceitarTudoReservado() {
        PecaInsumo peca = PecaInsumo.novo(NOME, null, 5, 5, PRECO);

        assertThat(peca.getReservado()).isEqualTo(5);
    }

    @Test
    @DisplayName("Deve validar quantidade antes do preço, na mesma ordem das mensagens da API")
    void deveValidarNaOrdemDaApi() {
        assertThatThrownBy(() -> PecaInsumo.novo(NOME, null, null, null, null))
                .isInstanceOf(DadoInvalidoException.class)
                .hasMessage("O campo quantidade é obrigatório");
    }

    @Test
    @DisplayName("Deve atualizar o estoque e o preço mantendo id e data de criação")
    void deveAtualizar() {
        PecaInsumo peca = PecaInsumo.reconstituir(7L, NOME, "filtro de oleo", null, 10, 2, new BigDecimal("30.00"),
                CRIADO_EM, CRIADO_EM);

        peca.atualizar(NOME, "Original", 12, 4, new BigDecimal("32.5"));

        assertThat(peca.getId()).isEqualTo(7L);
        assertThat(peca.getQuantidade()).isEqualTo(12);
        assertThat(peca.getReservado()).isEqualTo(4);
        assertThat(peca.getPreco()).isEqualTo(new BigDecimal("32.50"));
        assertThat(peca.getDescricao()).isEqualTo("Original");
        assertThat(peca.getCriadoEm()).isEqualTo(CRIADO_EM);
    }

    @Test
    @DisplayName("Atualização inválida não deve deixar o estoque incoerente")
    void naoDeveAlterarComAtualizacaoInvalida() {
        PecaInsumo peca = PecaInsumo.reconstituir(7L, NOME, "filtro de oleo", null, 10, 2, new BigDecimal("30.00"),
                CRIADO_EM, CRIADO_EM);

        assertThatThrownBy(() -> peca.atualizar(NOME, null, 3, 4, PRECO))
                .isInstanceOf(DadoInvalidoException.class);

        assertThat(peca.getQuantidade()).isEqualTo(10);
        assertThat(peca.getReservado()).isEqualTo(2);
    }
}
