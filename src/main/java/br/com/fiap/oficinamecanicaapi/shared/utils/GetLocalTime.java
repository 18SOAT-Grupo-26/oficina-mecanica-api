package br.com.fiap.oficinamecanicaapi.shared.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class GetLocalTime {

    public static LocalDateTime getLocalTime() {
        Instant instant = Instant.now();
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}
