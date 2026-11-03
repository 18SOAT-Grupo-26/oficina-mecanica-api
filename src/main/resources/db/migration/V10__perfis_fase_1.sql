-- Perfis da fase 1 (reunião de 25/10): ADMIN, FUNCIONARIO e CLIENTE. Gerente, mecânico, estoquista e atendente
-- ficam para a fase 2. Os perfis antigos são renomeados no lugar, então os ids e os logins existentes são
-- preservados; quem era MANAGER passa a FUNCIONARIO.

update perfis
set nome       = 'ADMIN',
    descricao  = 'Administrador: acesso total, inclusive exclusões e cadastro de usuários',
    updated_at = now()
where nome = 'ADMINISTRATOR';

update perfis
set nome       = 'FUNCIONARIO',
    descricao  = 'Funcionário da oficina: cadastros (sem excluir) e ordens de serviço',
    updated_at = now()
where nome = 'OPERATOR';

update usuarios
set perfil_id = (select id from perfis where nome = 'FUNCIONARIO')
where perfil_id = (select id from perfis where nome = 'MANAGER');

delete from perfil_permissoes
where perfil_id = (select id from perfis where nome = 'MANAGER');

delete from perfis where nome = 'MANAGER';

insert into perfis (nome, descricao, created_at, updated_at)
values ('CLIENTE', 'Cliente da oficina: consulta as próprias ordens de serviço', now(), now())
on conflict (nome) do nothing;

-- O usuário CLIENTE fica ligado ao cadastro do cliente, para só enxergar as próprias OS (um login por cliente)
alter table usuarios
    add column cliente_id bigint constraint fk_usuarios_cliente references clientes,
    add constraint uk_usuarios_cliente unique (cliente_id);
