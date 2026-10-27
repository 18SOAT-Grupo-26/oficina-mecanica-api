-- O cadastro de identidade passa a aceitar CPF ou CNPJ num campo único, no mesmo padrão de clientes.
-- O tipo é deduzido pela aplicação (Documento.tipo()) e a check constraint impede tipo e tamanho divergentes.

alter table identities rename column cpf to documento;

alter table identities alter column documento type varchar(14);

alter table identities
    add column tipo_documento varchar(4);

update identities
set tipo_documento = 'CPF'
where documento is not null;

alter table identities
    alter column documento set not null,
alter
column tipo_documento set not null,
    add constraint uk_identities_documento unique (documento),
    add constraint ck_identities_tipo_documento check (tipo_documento in ('CPF', 'CNPJ')),
    add constraint ck_identities_documento_tipo check (
        (tipo_documento = 'CPF' and length(documento) = 11) or
        (tipo_documento = 'CNPJ' and length(documento) = 14)
    );
