package br.com.fiap.oficinamecanicaapi.core.usecase.auth;

import br.com.fiap.oficinamecanicaapi.shared.exception.LimiteExcedidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimiteTentativasLoginTest {

    private static final int MAX_TENTATIVAS = 5;
    private static final long JANELA_MINUTOS = 15;
    private static final String IDENTIFICADOR = "ana@oficina.com.br";
    private static final String OUTRO_IDENTIFICADOR = "bruno";
    private static final Instant INICIO = Instant.parse("2026-10-27T12:00:00Z");

    private RelogioAjustavel relogio;
    private LimiteTentativasLogin limite;

    @BeforeEach
    void setUp() {
        relogio = new RelogioAjustavel(INICIO);
        limite = new LimiteTentativasLogin(relogio, MAX_TENTATIVAS, JANELA_MINUTOS);
    }

    @Test
    @DisplayName("Deve liberar o login enquanto as falhas estiverem abaixo do limite")
    void deveLiberarAbaixoDoLimite() {
        registrarFalhas(IDENTIFICADOR, MAX_TENTATIVAS - 1);

        assertDoesNotThrow(() -> limite.verificarBloqueio(IDENTIFICADOR));
    }

    @Test
    @DisplayName("Deve bloquear com 429 ao atingir o limite de falhas na janela")
    void deveBloquearAoAtingirLimite() {
        registrarFalhas(IDENTIFICADOR, MAX_TENTATIVAS);

        LimiteExcedidoException ex = assertThrows(LimiteExcedidoException.class,
                () -> limite.verificarBloqueio(IDENTIFICADOR));

    }

    @Test
    @DisplayName("Deve liberar de novo depois que a janela expirar")
    void deveLiberarAposJanela() {
        registrarFalhas(IDENTIFICADOR, MAX_TENTATIVAS);
        relogio.avancar(Duration.ofMinutes(JANELA_MINUTOS));

        assertDoesNotThrow(() -> limite.verificarBloqueio(IDENTIFICADOR));
    }

    @Test
    @DisplayName("Deve zerar a contagem quando o login der certo")
    void deveZerarAposSucesso() {
        registrarFalhas(IDENTIFICADOR, MAX_TENTATIVAS - 1);
        limite.registrarSucesso(IDENTIFICADOR);
        registrarFalhas(IDENTIFICADOR, MAX_TENTATIVAS - 1);

        assertDoesNotThrow(() -> limite.verificarBloqueio(IDENTIFICADOR));
    }

    @Test
    @DisplayName("Deve contar o mesmo identificador ignorando maiúsculas e espaços")
    void deveIgnorarMaiusculasEEspacos() {
        registrarFalhas(" ANA@Oficina.com.br ", MAX_TENTATIVAS);

        assertThrows(LimiteExcedidoException.class, () -> limite.verificarBloqueio(IDENTIFICADOR));
    }

    @Test
    @DisplayName("Não deve bloquear outra conta quando uma estiver bloqueada")
    void naoDeveAfetarOutraConta() {
        registrarFalhas(IDENTIFICADOR, MAX_TENTATIVAS);

        assertDoesNotThrow(() -> limite.verificarBloqueio(OUTRO_IDENTIFICADOR));
    }

    @Test
    @DisplayName("Deve descartar entradas expiradas quando o número de entradas passar do limite")
    void deveDescartarExpiradasAcimaDoLimite() {
        for (int i = 0; i <= LimiteTentativasLogin.LIMITE_ENTRADAS; i++) {
            limite.registrarFalha("usuario" + i);
        }
        relogio.avancar(Duration.ofMinutes(JANELA_MINUTOS));

        limite.registrarFalha(IDENTIFICADOR);

        assertThat(limite.totalEntradas()).isEqualTo(1);
    }

    private void registrarFalhas(String identificador, int quantidade) {
        for (int i = 0; i < quantidade; i++) {
            limite.registrarFalha(identificador);
        }
    }

    /** Relógio de teste que só anda quando mandado. */
    private static final class RelogioAjustavel extends Clock {

        private Instant agora;

        RelogioAjustavel(Instant inicio) {
            this.agora = inicio;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
