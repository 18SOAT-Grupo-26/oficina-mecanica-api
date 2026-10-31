# DER: banco de dados

Modelo físico do PostgreSQL da fase 1, do jeito que fica depois das migrations V1 a V11. Como a aplicação sobe com
`ddl-auto: validate`, as entidades JPA precisam bater com essas tabelas. A escolha do banco e o histórico das migrations
estão na [ADR 002](../adr/0002-escolha-do-banco-de-dados.md); as transações e a reserva de estoque, na
[ADR 003](../adr/0003-estrategia-de-transacoes.md); os perfis, na [ADR 004](../adr/0004-seguranca.md).

Convenções do diagrama:

- `PK`, `FK` e `UK` marcam chave primária, chave estrangeira e `UNIQUE`. Os comentários entre aspas resumem os `CHECK`
  e as regras de cada coluna.
- Os tipos estão simplificados: `varchar` sem o tamanho, `numeric` é `numeric(12, 2)` e `timestamptz` é
  `timestamp(6) with time zone`.
- A `flyway_schema_history`, a tabela de controle do Flyway, fica de fora.

```mermaid
erDiagram
    perfis ||--o{ usuarios : "define o acesso de"
    perfis ||--o{ perfil_permissoes : "agrupa"
    permissoes ||--o{ perfil_permissoes : "compõe"
    clientes |o--o| usuarios : "acessa como"
    clientes ||--o{ veiculos : "possui"
    clientes ||--o{ ordem_servico : "solicita"
    veiculos ||--o{ ordem_servico : "recebe"
    ordem_servico ||--o{ item_servico_os : "inclui"
    servico ||--o{ item_servico_os : "é cobrado em"
    ordem_servico ||--o{ item_peca_os : "consome"
    peca_insumo ||--o{ item_peca_os : "é usada em"
    ordem_servico ||--o{ historico_status_os : "registra"

    perfis {
        bigint id PK
        varchar nome UK "ADMIN, FUNCIONARIO, CLIENTE"
        varchar descricao
        timestamptz created_at
        timestamptz updated_at
    }
    permissoes {
        bigint id PK
        varchar nome UK
        varchar descricao
        timestamptz created_at
        timestamptz updated_at
    }
    perfil_permissoes {
        bigint perfil_id PK, FK
        bigint permissao_id PK, FK
    }
    usuarios {
        bigint id PK
        varchar nome
        varchar email UK
        varchar nome_usuario UK
        varchar senha "hash bcrypt"
        boolean troca_senha_obrigatoria
        bigint perfil_id FK
        bigint cliente_id FK, UK "só para o perfil CLIENTE"
        boolean ativo
        timestamptz created_at
        timestamptz updated_at
    }
    clientes {
        bigint id PK
        varchar nome
        varchar documento UK "11 (CPF) ou 14 (CNPJ)"
        varchar tipo_documento "CPF ou CNPJ"
        varchar email
        varchar telefone
        varchar logradouro "endereço (objeto de valor embutido)"
        varchar numero "endereço"
        varchar bairro "endereço"
        varchar cidade "endereço"
        varchar estado "endereço"
        varchar cep "endereço"
        varchar complemento "endereço"
        boolean ativo "delete lógico"
        timestamptz created_at
        timestamptz updated_at
    }
    veiculos {
        bigint id PK
        varchar placa UK "7 caracteres"
        varchar marca
        varchar modelo
        int ano
        bigint cliente_id FK
        boolean ativo "delete lógico"
        timestamptz created_at
        timestamptz updated_at
    }
    servico {
        bigint id PK
        varchar nome
        varchar nome_normalizado UK
        varchar descricao
        numeric preco
        boolean ativo "delete lógico"
        timestamptz created_at
        timestamptz updated_at
    }
    peca_insumo {
        bigint id PK
        varchar nome
        varchar nome_normalizado UK
        varchar descricao
        int quantidade "estoque físico"
        int reservado "reservado para orçamentos"
        numeric preco
        boolean ativo "delete lógico"
        timestamptz created_at
        timestamptz updated_at
    }
    ordem_servico {
        bigint id PK
        bigint cliente_id FK
        bigint veiculo_id FK
        varchar status "RECEBIDA a ENTREGUE, ou CANCELADA"
        timestamptz created_at
        timestamptz updated_at
    }
    item_servico_os {
        bigint id PK
        bigint ordem_servico_id FK
        bigint servico_id FK
        varchar descricao "copiada do serviço"
        numeric preco_unitario "copiado do serviço, >= 0"
        int quantidade "> 0"
    }
    item_peca_os {
        bigint id PK
        bigint ordem_servico_id FK
        bigint peca_insumo_id FK
        varchar descricao "copiada da peça"
        numeric preco_unitario "copiado da peça, >= 0"
        int quantidade "> 0"
    }
    historico_status_os {
        bigint id_historico PK
        bigint id_os FK
        varchar status_antigo "nulo na abertura"
        varchar status_atualizado
        timestamptz data_criacao
    }
```

## Como ler

- **Usuário e perfil:** todo usuário tem exatamente 1 perfil. Nesta fase, a autorização é feita por perfil, e nenhuma
  migration popula `permissoes` nem `perfil_permissoes`. As tabelas ficam prontas para permissões mais finas na fase 2.
- **Usuário e cliente:** o `cliente_id` é opcional e único. Só o usuário com perfil `CLIENTE` fica ligado a um cliente,
  e cada cliente tem no máximo um usuário. É por esse vínculo que o cliente vê só as próprias OS.
- **Cliente:** tem vários veículos e várias OS. O endereço não tem tabela própria: é o objeto de valor `Endereco`,
  gravado nas colunas de `clientes` (`EnderecoEmbeddable`).
- **OS:** pertence a 1 cliente e a 1 veículo e tem vários itens de serviço, itens de peça e registros de histórico.
  O `status` só aceita os valores do `StatusOrdemServico` (`CHECK` no banco).
- **Itens da OS:** guardam a `descricao` e o `preco_unitario` do momento da inclusão, para o orçamento não mudar quando
  o cadastro de serviços e peças muda.
- **Estoque:** `quantidade` é o estoque físico e `reservado` é o que já foi separado para orçamentos. O disponível é
  `quantidade - reservado`. Os `UPDATE` de cada passo estão nos [diagramas de sequência da OS](sequencia-ordem-servico.md).
- **Histórico:** cada mudança de status gera uma linha em `historico_status_os`. É a base do tempo médio de execução
  (de `EM_EXECUCAO` a `FINALIZADA`).
- **Delete lógico:** clientes, veículos, serviços e peças são desativados pelo `ativo` (o catálogo desde a V11), e a OS
  vai para `CANCELADA`. Nada disso é apagado fisicamente: a OS antiga continua apontando para o serviço e a peça da
  época, que só deixam de aparecer no catálogo e de poder entrar em OS nova.

As tabelas `identidades`, `contatos` e `enderecos` (removidas na V8) e `servico_peca_insumo` (removida na V9) aparecem
nas migrations antigas, mas não existem mais.
