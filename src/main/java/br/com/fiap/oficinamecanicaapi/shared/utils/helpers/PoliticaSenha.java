package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.shared.exception.DadoInvalidoException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PoliticaSenha {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    /**
     * Specials we deliberately allow. We exclude the high-risk ones:
     * '  "  `  \  ;
     * That keeps the password safe even if some downstream layer accidentally
     * builds an unparametrized query.
     */
    public static final String ALLOWED_SPECIALS = "!@#$%^&*()-_=+[]{}<>?/,.:|~";

    private static final Pattern ALLOWED_CHARS =
            Pattern.compile("^[A-Za-z0-9" + Pattern.quote(ALLOWED_SPECIALS) + "]+$");

    private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern HAS_SPECIAL =
            Pattern.compile("[" + Pattern.quote(ALLOWED_SPECIALS) + "]");

    public String allowedSpecials() {
        return ALLOWED_SPECIALS;
    }

    public void validar(String raw) {
        if (raw == null) {
            throw new DadoInvalidoException("A senha é obrigatória");
        }
        if (raw.length() < MIN_LENGTH) {
            throw new DadoInvalidoException("A senha deve ter no mínimo " + MIN_LENGTH + " caracteres");
        }
        if (raw.length() > MAX_LENGTH) {
            throw new DadoInvalidoException("A senha deve ter no máximo " + MAX_LENGTH + " caracteres");
        }
        if (!ALLOWED_CHARS.matcher(raw).matches()) {
            throw new DadoInvalidoException("A senha contém caracteres não permitidos. Especiais permitidos: "
                            + ALLOWED_SPECIALS);
        }
        if (!HAS_UPPER.matcher(raw).find()) {
            throw new DadoInvalidoException("A senha deve conter uma letra maiúscula");
        }
        if (!HAS_LOWER.matcher(raw).find()) {
            throw new DadoInvalidoException("A senha deve conter uma letra minúscula");
        }
        if (!HAS_DIGIT.matcher(raw).find()) {
            throw new DadoInvalidoException("A senha deve conter um número");
        }
        if (!HAS_SPECIAL.matcher(raw).find()) {
            throw new DadoInvalidoException("A senha deve conter um destes caracteres especiais: " + ALLOWED_SPECIALS);
        }
    }

}
