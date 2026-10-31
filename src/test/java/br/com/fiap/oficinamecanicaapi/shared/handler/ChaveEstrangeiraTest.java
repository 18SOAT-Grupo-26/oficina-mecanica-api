package br.com.fiap.oficinamecanicaapi.shared.handler;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class ChaveEstrangeiraTest {

    private static final String SQLSTATE_UNICIDADE = "23505";

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "fk_item_servico_os_servico | O serviço está em uma ordem de serviço e não pode ser excluído",
            "fk_item_peca_os_peca       | O insumo está em uma ordem de serviço e não pode ser excluído",
            "fk_qualquer_outra          | O registro está em uso e não pode ser excluído"})
    @DisplayName("Violação de chave estrangeira deve virar a mensagem da constraint, ou a padrão")
    void deveTraduzirChaveEstrangeira(String constraint, String mensagem) {
        assertThat(ChaveEstrangeira.mensagem(integridade(ChaveEstrangeira.SQLSTATE, constraint))).contains(mensagem);
    }

    @Test
    @DisplayName("Violação de unicidade não é chave estrangeira")
    void naoDeveTraduzirUnicidade() {
        assertThat(ChaveEstrangeira.mensagem(integridade(SQLSTATE_UNICIDADE, "uk_servico_nome"))).isEmpty();
    }

    @Test
    @DisplayName("Erro sem causa do Hibernate não é chave estrangeira")
    void naoDeveTraduzirSemCausa() {
        assertThat(ChaveEstrangeira.mensagem(new DataIntegrityViolationException("sem causa"))).isEmpty();
    }

    static DataIntegrityViolationException integridade(String sqlState, String constraint) {
        var sql = new SQLException("violação", sqlState);
        return new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("could not execute statement", sql, constraint));
    }
}
