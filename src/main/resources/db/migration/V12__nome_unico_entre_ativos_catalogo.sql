-- O nome de serviço e peça passa a ser único só entre os ativos: desativar um item (V11) libera o nome para um
-- cadastro novo, e o item antigo continua desativado, ligado às OS que já o usam. Os nomes das constraints são
-- mantidos nos índices, para os logs e as mensagens de integridade continuarem reconhecendo.

alter table servico
    drop constraint uk_servico_nome_normalizado;

create unique index uk_servico_nome_normalizado on servico (nome_normalizado) where ativo;

alter table peca_insumo
    drop constraint uk_peca_insumo_nome_normalizado;

create unique index uk_peca_insumo_nome_normalizado on peca_insumo (nome_normalizado) where ativo;
