package br.com.fiap.oficinamecanicaapi.shared.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class NormalizadorNome {

    private NormalizadorNome() {
    }

    /**
     * Ignora acentos, maiúsculas/minúsculas e espaços extras.
     */
    public static String normalizar(String nome) {
        return Normalizer.normalize(nome.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}
