package br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.EM_DIAGNOSTICO;
import static br.com.fiap.oficinamecanicaapi.core.domain.ordemservico.model.StatusOrdemServico.RECEBIDA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistroHistoricoTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-10-24T08:00:00-03:00");

    @Test
    @DisplayName("Deve comparar registros por valor")
    void deveCompararPorValor() {
        assertThat(new RegistroHistorico(RECEBIDA, EM_DIAGNOSTICO, AGORA))
                .isEqualTo(new RegistroHistorico(RECEBIDA, EM_DIAGNOSTICO, AGORA));
    }

    @Test
    @DisplayName("Deve aceitar status antigo vazio só no registro da abertura (RECEBIDA)")
    void deveAceitarStatusAntigoVazioSoNaAbertura() {
        assertThat(new RegistroHistorico(null, RECEBIDA, AGORA).statusAntigo()).isNull();

        ApplicationException semAntigo = assertThrows(ApplicationException.class,
                () -> new RegistroHistorico(null, EM_DIAGNOSTICO, AGORA));
        assertThat(semAntigo.getErrorCode()).isEqualTo(400);
        assertThat(semAntigo.getMessage()).isEqualTo("O registro de histórico exige o status antigo, exceto na abertura");
    }

    @Test
    @DisplayName("Deve rejeitar registro sem status atualizado ou sem data/hora com erro 400")
    void deveRejeitarCamposNulos() {
        ApplicationException semStatus = assertThrows(ApplicationException.class,
                () -> new RegistroHistorico(RECEBIDA, null, AGORA));
        ApplicationException semData = assertThrows(ApplicationException.class,
                () -> new RegistroHistorico(RECEBIDA, EM_DIAGNOSTICO, null));

        assertThat(semStatus.getErrorCode()).isEqualTo(400);
        assertThat(semData.getErrorCode()).isEqualTo(400);
        assertThat(semData.getMessage()).isEqualTo("O registro de histórico exige status atualizado e data/hora");
    }
}
