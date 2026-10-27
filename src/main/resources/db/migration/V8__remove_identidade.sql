-- O usuário passa a guardar nome e e-mail, os únicos dados pessoais usados pelo sistema (login por e-mail e
-- claims do JWT). A identidade, o contato e o endereço deixam de existir (minimização de dados, LGPD).
-- Os logins existentes são preservados: nome e e-mail vêm das tabelas antigas.

alter table usuarios add column nome varchar(255);
alter table usuarios add column email varchar(255);

update usuarios u
set nome  = i.nome_completo,
    email = c.email
from identidades i
join contatos c on c.id = i.contato_id
where i.id = u.identidade_id;

-- A unicidade de nome_usuario já vem da V7
alter table usuarios
    alter column nome set not null,
    alter column email set not null,
    add constraint uk_usuarios_email unique (email);

-- As constraints da coluna (uk_/fk_) são removidas junto com ela
alter table usuarios drop column identidade_id;

drop table identidades;
drop table contatos;
drop table enderecos;
