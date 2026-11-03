package br.com.fiap.oficinamecanicaapi.core.domain.auth.model;

import java.util.Arrays;

/**
 * Chave de assinatura do JWT já resolvida e validada na subida (ver SegredoJwtConfig). Classe, e não record, para
 * copiar o array na entrada e na saída e nunca expor a chave no toString.
 */
public final class SegredoJwt {

    private final byte[] chave;

    public SegredoJwt(byte[] chave) {
        this.chave = Arrays.copyOf(chave, chave.length);
    }

    public byte[] chave() {
        return Arrays.copyOf(chave, chave.length);
    }

    @Override
    public String toString() {
        return "SegredoJwt[" + chave.length + " bytes]";
    }
}
