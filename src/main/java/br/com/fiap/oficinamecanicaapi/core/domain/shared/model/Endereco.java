package br.com.fiap.oficinamecanicaapi.core.domain.shared.model;

public record Endereco(
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep,
        String complemento
) {
}
