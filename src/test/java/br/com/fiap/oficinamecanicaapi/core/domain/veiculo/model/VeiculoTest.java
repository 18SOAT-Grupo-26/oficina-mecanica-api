package br.com.fiap.oficinamecanicaapi.core.domain.veiculo.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import br.com.fiap.oficinamecanicaapi.shared.utils.GetLocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VeiculoTest {

    private static final Placa PLACA = new Placa("BRA2E19");
    private static final String MARCA = "Volkswagen";
    private static final String MODELO = "Gol";
    private static final Integer ANO = 2020;
    private static final Long CLIENTE_ID = 10L;

    @Test
    @DisplayName("Deve criar veículo novo ativo e sem id, removendo espaços de marca e modelo")
    void deveCriarVeiculoNovo() {
        Veiculo veiculo = Veiculo.novo(PLACA, "  " + MARCA + " ", MODELO, ANO, CLIENTE_ID);

        assertThat(veiculo.getId()).isNull();
        assertThat(veiculo.isAtivo()).isTrue();
        assertThat(veiculo.getMarca()).isEqualTo(MARCA);
        assertThat(veiculo.getClienteId()).isEqualTo(CLIENTE_ID);
    }

    @Test
    @DisplayName("Deve aceitar os anos limite: 1900 e o ano seguinte ao atual")
    void deveAceitarAnosLimite() {
        int anoSeguinte = Year.now(GetLocalTime.FUSO).getValue() + 1;

        assertThat(Veiculo.novo(PLACA, MARCA, MODELO, Veiculo.ANO_MINIMO, CLIENTE_ID).getAno())
                .isEqualTo(Veiculo.ANO_MINIMO);
        assertThat(Veiculo.novo(PLACA, MARCA, MODELO, anoSeguinte, CLIENTE_ID).getAno())
                .isEqualTo(anoSeguinte);
    }

    @Test
    @DisplayName("Deve rejeitar ano anterior a 1900 com erro 400")
    void deveRejeitarAnoMuitoAntigo() {
        DadoInvalidoException exception = assertThrows(DadoInvalidoException.class,
                () -> Veiculo.novo(PLACA, MARCA, MODELO, Veiculo.ANO_MINIMO - 1, CLIENTE_ID));

        assertThat(exception.getMessage()).startsWith("Ano inválido");
    }

    @Test
    @DisplayName("Deve rejeitar ano posterior ao ano seguinte ao atual com erro 400")
    void deveRejeitarAnoFuturo() {
        int anoInvalido = Year.now(GetLocalTime.FUSO).getValue() + 2;

        DadoInvalidoException exception = assertThrows(DadoInvalidoException.class,
                () -> Veiculo.novo(PLACA, MARCA, MODELO, anoInvalido, CLIENTE_ID));

    }

    @Test
    @DisplayName("Deve rejeitar veículo sem marca, modelo, placa ou cliente com erro 400")
    void deveRejeitarCamposObrigatorios() {
        assertThrows(DadoInvalidoException.class, () -> Veiculo.novo(PLACA, " ", MODELO, ANO, CLIENTE_ID));
        assertThrows(DadoInvalidoException.class, () -> Veiculo.novo(PLACA, MARCA, null, ANO, CLIENTE_ID));
        assertThrows(DadoInvalidoException.class, () -> Veiculo.novo(null, MARCA, MODELO, ANO, CLIENTE_ID));
        assertThrows(DadoInvalidoException.class, () -> Veiculo.novo(PLACA, MARCA, MODELO, ANO, null));
        assertThrows(DadoInvalidoException.class, () -> Veiculo.novo(PLACA, MARCA, MODELO, null, CLIENTE_ID));
    }

    @Test
    @DisplayName("Deve atualizar os dados e desativar o veículo")
    void deveAtualizarEDesativar() {
        Veiculo veiculo = Veiculo.novo(PLACA, MARCA, MODELO, ANO, CLIENTE_ID);
        Placa novaPlaca = new Placa("ABC1234");

        veiculo.atualizar(novaPlaca, "Fiat", "Uno", 2015, 20L);
        veiculo.desativar();

        assertThat(veiculo.getPlaca()).isEqualTo(novaPlaca);
        assertThat(veiculo.getModelo()).isEqualTo("Uno");
        assertThat(veiculo.getClienteId()).isEqualTo(20L);
        assertThat(veiculo.isAtivo()).isFalse();
    }
}
