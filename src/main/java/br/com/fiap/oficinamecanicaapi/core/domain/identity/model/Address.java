package br.com.fiap.oficinamecanicaapi.core.domain.identity.model;

public record Address(
        String street,
        String number,
        String district,
        String city,
        String state,
        String zipCode,
        String complement
) {
}
