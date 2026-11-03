-- O login busca o usuário pelo nome; com dois iguais a busca falha e o login responde 404 para os dois.
-- Bancos locais criados antes desta versão podem ter nomes repetidos: o mais antigo mantém o nome e os
-- demais ganham o sufixo _<id> (ex.: admin_4), para a constraint poder ser criada.

update usuarios u
set nome_usuario = u.nome_usuario || '_' || u.id
where exists (select 1
              from usuarios o
              where o.nome_usuario = u.nome_usuario
                and o.id < u.id);

alter table usuarios
    add constraint uk_usuarios_nome_usuario unique (nome_usuario);
