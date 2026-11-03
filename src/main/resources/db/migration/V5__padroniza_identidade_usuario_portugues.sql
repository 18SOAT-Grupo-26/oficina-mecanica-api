-- Padroniza em português as tabelas de identidade, usuário e perfis, como clientes e veículos.
-- Só renomeia (dados preservados). created_at/updated_at seguem o padrão das demais tabelas.

-- Permissões e perfis ----------------------------------------------------------------------------

alter table permissions rename to permissoes;
alter table permissoes rename column name to nome;
alter table permissoes rename column description to descricao;
alter table permissoes rename constraint permissions_pkey to permissoes_pkey;
alter table permissoes rename constraint uk_permissions_name to uk_permissoes_nome;

alter table roles rename to perfis;
alter table perfis rename column name to nome;
alter table perfis rename column description to descricao;
alter table perfis rename constraint roles_pkey to perfis_pkey;
alter table perfis rename constraint uk_roles_name to uk_perfis_nome;

alter table role_permissions rename to perfil_permissoes;
alter table perfil_permissoes rename column role_id to perfil_id;
alter table perfil_permissoes rename column permission_id to permissao_id;
alter table perfil_permissoes rename constraint role_permissions_pkey to perfil_permissoes_pkey;
alter table perfil_permissoes rename constraint fk_role_permissions_role to fk_perfil_permissoes_perfil;
alter table perfil_permissoes rename constraint fk_role_permissions_permission to fk_perfil_permissoes_permissao;

-- Contato, endereço e identidade -----------------------------------------------------------------

alter table contacts rename to contatos;
alter table contatos rename column phone_number to telefone;
alter table contatos rename constraint contacts_pkey to contatos_pkey;

alter table addresses rename to enderecos;
alter table enderecos rename column street to logradouro;
alter table enderecos rename column number to numero;
alter table enderecos rename column district to bairro;
alter table enderecos rename column city to cidade;
alter table enderecos rename column state to estado;
alter table enderecos rename column zip_code to cep;
alter table enderecos rename column complement to complemento;
alter table enderecos rename constraint addresses_pkey to enderecos_pkey;

alter table identities rename to identidades;
alter table identidades rename column full_name to nome_completo;
alter table identidades rename column given_name to primeiro_nome;
alter table identidades rename column date_of_birth to data_nascimento;
alter table identidades rename column contact_id to contato_id;
alter table identidades rename column address_id to endereco_id;
alter table identidades rename column active to ativo;
alter table identidades rename constraint identities_pkey to identidades_pkey;
alter table identidades rename constraint uk_identities_contact to uk_identidades_contato;
alter table identidades rename constraint fk_identities_contact to fk_identidades_contato;
alter table identidades rename constraint uk_identities_address to uk_identidades_endereco;
alter table identidades rename constraint fk_identities_address to fk_identidades_endereco;
alter table identidades rename constraint uk_identities_documento to uk_identidades_documento;
alter table identidades rename constraint ck_identities_tipo_documento to ck_identidades_tipo_documento;
alter table identidades rename constraint ck_identities_documento_tipo to ck_identidades_documento_tipo;

-- Usuário ----------------------------------------------------------------------------------------

alter table users rename to usuarios;
alter table usuarios rename column identity_id to identidade_id;
alter table usuarios rename column username to nome_usuario;
alter table usuarios rename column password to senha;
alter table usuarios rename column change_required to troca_senha_obrigatoria;
alter table usuarios rename column role_id to perfil_id;
alter table usuarios rename column active to ativo;
alter table usuarios rename constraint users_pkey to usuarios_pkey;
alter table usuarios rename constraint uk_users_identity to uk_usuarios_identidade;
alter table usuarios rename constraint fk_users_identity to fk_usuarios_identidade;
alter table usuarios rename constraint fk_users_role to fk_usuarios_perfil;
