package br.com.fiap.oficinamecanicaapi.shared.utils;

public final class MascaraDados {

    private static final String MASCARA = "***";

    private MascaraDados() {
    }

    /**
     * Mascara o identificador de login para log (LGPD): e-mail mantém a 1ª letra e o domínio
     * ({@code a***@oficina.com.br}); nome de usuário mantém até 2 letras ({@code ad***}).
     */
    public static String mascararIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return MASCARA;
        }
        String valor = identificador.trim();
        int arroba = valor.indexOf('@');
        if (arroba > 0) {
            return valor.charAt(0) + MASCARA + valor.substring(arroba);
        }
        return valor.substring(0, Math.min(2, valor.length())) + MASCARA;
    }
}
