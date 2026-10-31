-- Serviço e peça passam a ser desativados em vez de apagados, como cliente e veículo (dívida B2). Assim a OS antiga
-- continua apontando para o item do catálogo da época, e o DELETE da API não esbarra mais nas FKs de
-- item_servico_os e item_peca_os. Os registros que já existem ficam ativos.

alter table servico
    add column ativo boolean not null default true;

alter table peca_insumo
    add column ativo boolean not null default true;
