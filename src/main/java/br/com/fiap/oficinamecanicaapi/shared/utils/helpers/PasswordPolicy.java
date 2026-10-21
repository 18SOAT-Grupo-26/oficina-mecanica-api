package br.com.fiap.oficinamecanicaapi.shared.utils.helpers;

import br.com.fiap.oficinamecanicaapi.shared.exception.ApplicationException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    /**
     * Specials we deliberately allow. We exclude the high-risk ones:
     *   '  "  `  \  ;
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

    public void validate(String raw) throws ApplicationException {
        if (raw == null) {
            throw new ApplicationException(400, "Password is required");
        }
        if (raw.length() < MIN_LENGTH) {
            throw new ApplicationException(400,
                    "Password must have at least " + MIN_LENGTH + " characters");
        }
        if (raw.length() > MAX_LENGTH) {
            throw new ApplicationException(400,
                    "Password must have at most " + MAX_LENGTH + " characters");
        }
        if (!ALLOWED_CHARS.matcher(raw).matches()) {
            throw new ApplicationException(400,
                    "Password contains disallowed characters. Allowed specials: "
                            + ALLOWED_SPECIALS);
        }
        if (!HAS_UPPER.matcher(raw).find()) {
            throw new ApplicationException(400, "Password must include an uppercase letter");
        }
        if (!HAS_LOWER.matcher(raw).find()) {
            throw new ApplicationException(400, "Password must include a lowercase letter");
        }
        if (!HAS_DIGIT.matcher(raw).find()) {
            throw new ApplicationException(400, "Password must include a digit");
        }
        if (!HAS_SPECIAL.matcher(raw).find()) {
            throw new ApplicationException(400,
                    "Password must include one of these specials: " + ALLOWED_SPECIALS);
        }
    }

}
