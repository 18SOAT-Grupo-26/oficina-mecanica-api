package br.com.fiap.oficinamecanicaapi.shared.utils;

/**
 * Brazilian CNPJ (Cadastro Nacional da Pessoa Jurídica) validator.
 *
 * <p>Supports both the numeric format and the alphanumeric format in force
 * since July 2026: 12 characters in {@code [A-Z0-9]} followed by 2 numeric
 * check digits. Each character is worth its ASCII code minus 48, which keeps
 * the classic numeric calculation unchanged.
 */
public final class CnpjValidator {

    private static final int LENGTH = 14;
    private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjValidator() {
    }

    /**
     * Returns an uppercase copy with only letters and digits. NULL stays NULL.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    /**
     * Validates the CNPJ by verifying both check digits.
     * Returns {@code false} for nulls, wrong length/format and repeated characters.
     */
    public static boolean isValid(String raw) {
        String c = normalize(raw);
        if (c == null || !c.matches("[A-Z0-9]{12}[0-9]{2}")) return false;
        if (c.chars().distinct().count() == 1) return false; // 00000000000000, 11111111111111, ...

        int firstCheck = checkDigit(c, FIRST_WEIGHTS);
        if (firstCheck != (c.charAt(12) - '0')) return false;

        int secondCheck = checkDigit(c, SECOND_WEIGHTS);
        return secondCheck == (c.charAt(13) - '0');
    }

    /**
     * Formats for display: 12ABC34501DE35 → 12.ABC.345/01DE-35.
     */
    public static String format(String raw) {
        String c = normalize(raw);
        if (c == null || c.length() != LENGTH) return raw;
        return c.substring(0, 2) + "." + c.substring(2, 5) + "." + c.substring(5, 8)
                + "/" + c.substring(8, 12) + "-" + c.substring(12);
    }

    private static int checkDigit(String c, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (c.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
