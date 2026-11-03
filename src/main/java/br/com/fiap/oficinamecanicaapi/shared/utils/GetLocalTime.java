package br.com.fiap.oficinamecanicaapi.shared.utils;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public class GetLocalTime {

    /**
     * Fuso da oficina: o mesmo horário no container e na máquina do desenvolvedor.
     */
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private GetLocalTime() {
    }

    public static OffsetDateTime getLocalTime() {
        return OffsetDateTime.now(FUSO);
    }

}
