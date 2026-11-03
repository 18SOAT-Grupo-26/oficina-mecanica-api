-- Perfis do enum Role (core/domain/user/model/Role.java). O cadastro de usuário (/auth/register)
-- exige que o perfil exista nesta tabela. Nenhum usuário é criado aqui: credenciais não vão para o repositório.

insert into roles (name, description, created_at, updated_at)
values ('ADMINISTRATOR', 'Administrador: acesso total às APIs administrativas', now(), now()),
       ('MANAGER', 'Gerente da oficina', now(), now()),
       ('OPERATOR', 'Operador/atendente da oficina', now(), now())
on conflict (name) do nothing;
