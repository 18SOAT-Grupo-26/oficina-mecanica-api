package br.com.fiap.oficinamecanicaapi.core.usecase.auth;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção contra força bruta no login: após {@code maxTentativas} falhas de um mesmo identificador dentro da
 * janela, o login dele responde 429 até a janela expirar. Conta também identificadores inexistentes, para o 429
 * não revelar quais contas existem. Em memória: cada instância conta separado e um reinício zera a contagem.
 */
@Component
public class LimiteTentativasLogin {

    /** Acima disso, entradas expiradas são descartadas antes de inserir (evita crescer sem limite). */
    static final int LIMITE_ENTRADAS = 10_000;

    private final Map<String, Tentativas> tentativas = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxTentativas;
    private final Duration janela;

    public LimiteTentativasLogin(Clock clock,
                                 @Value("${api.login.max-tentativas:5}") int maxTentativas,
                                 @Value("${api.login.janela-minutos:15}") long janelaMinutos) {
        this.clock = clock;
        this.maxTentativas = maxTentativas;
        this.janela = Duration.ofMinutes(janelaMinutos);
    }

    public void verificarBloqueio(String identificador) throws ApplicationException {
        String chave = chave(identificador);
        Tentativas atual = tentativas.get(chave);
        if (atual == null) {
            return;
        }
        if (atual.expirada(agora(), janela)) {
            tentativas.remove(chave, atual);
            return;
        }
        if (atual.falhas() >= maxTentativas) {
            throw new ApplicationException(429, "Muitas tentativas de login. Tente novamente em alguns minutos.");
        }
    }

    public void registrarFalha(String identificador) {
        if (tentativas.size() > LIMITE_ENTRADAS) {
            descartarExpiradas();
        }
        Instant agora = agora();
        tentativas.compute(chave(identificador), (chave, atual) ->
                atual == null || atual.expirada(agora, janela)
                        ? new Tentativas(1, agora)
                        : new Tentativas(atual.falhas() + 1, atual.inicioJanela()));
    }

    public void registrarSucesso(String identificador) {
        tentativas.remove(chave(identificador));
    }

    int totalEntradas() {
        return tentativas.size();
    }

    private void descartarExpiradas() {
        Instant agora = agora();
        tentativas.values().removeIf(atual -> atual.expirada(agora, janela));
    }

    private Instant agora() {
        return clock.instant();
    }

    private static String chave(String identificador) {
        return identificador == null ? "" : identificador.trim().toLowerCase(Locale.ROOT);
    }

    private record Tentativas(int falhas, Instant inicioJanela) {

        boolean expirada(Instant agora, Duration janela) {
            return !agora.isBefore(inicioJanela.plus(janela));
        }
    }
}
