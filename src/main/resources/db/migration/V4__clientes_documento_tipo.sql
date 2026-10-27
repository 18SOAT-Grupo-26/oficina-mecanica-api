-- Garante no banco a coerência entre o tipo e o tamanho do documento do cliente, como em identities (V3).

alter table clientes
    add constraint ck_clientes_documento_tipo check (
        (tipo_documento = 'CPF' and length(documento) = 11) or
        (tipo_documento = 'CNPJ' and length(documento) = 14)
        );
