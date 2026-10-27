package br.com.fiap.oficinamecanicaapi.shared.utils;

/**
 * Brazilian CPF (Cadastro de Pessoas Físicas) validator.
 *
 * <p>CPF is an 11-digit document with two verification digits at the end.
 * We always store and compare in digits-only form — the display mask
 * {@code XXX.XXX.XXX-XX} is a presentation concern handled by the UI.
 */
public final class CpfValidator {

    private CpfValidator() {
    }

    /**
     * Returns a digits-only copy of the input. NULL stays NULL.
     */
    public static String digits(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("\\D", "");
    }

    /**
     * Validates the CPF by verifying both check digits.
     * Returns {@code false} for nulls, wrong length and known invalid patterns
     * (e.g. "111.111.111-11" is mathematically valid but globally rejected).
     */
    public static boolean isValid(String raw) {
        String d = digits(raw);
        if (d == null || d.length() != 11) return false;
        if (d.chars().distinct().count() == 1) return false; // 00000000000, 11111111111, ...

        try {
            // First check digit
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += (d.charAt(i) - '0') * (10 - i);
            }
            int firstCheck = (sum * 10) % 11;
            if (firstCheck == 10) firstCheck = 0;
            if (firstCheck != (d.charAt(9) - '0')) return false;

            // Second check digit
            sum = 0;
            for (int i = 0; i < 10; i++) {
                sum += (d.charAt(i) - '0') * (11 - i);
            }
            int secondCheck = (sum * 10) % 11;
            if (secondCheck == 10) secondCheck = 0;
            return secondCheck == (d.charAt(10) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Formats for display: 12345678901 → 123.456.789-01.
     */
    public static String format(String raw) {
        String d = digits(raw);
        if (d == null || d.length() != 11) return raw;
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }
}
