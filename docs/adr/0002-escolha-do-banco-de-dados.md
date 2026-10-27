# ADR 002 — Escolha do banco de dados

- **Status:** Proposto
- **Data:** 2026-10-24
- **Responsável:** Ana Albuquerque

## Contexto

A API precisa guardar os dados da oficina: clientes, veículos, serviços, peças e insumos, ordens de serviço (OS) e
usuários com seus perfis e permissões. Esses dados são muito relacionados entre si:

- Um veículo pertence a um cliente ("pensando em banco de dados, o veículo é do cliente", Event Storming de 16/10).
- Uma OS reúne cliente, veículo, serviços e peças. Um serviço tem as peças e insumos que usa.
- Usuários têm um perfil, e cada perfil tem uma lista de permissões no padrão CRUD (ex.: `Funcionario_CREATE`), decidido
  em 21/10.

Algumas regras de negócio dependem de consistência e integridade dos dados:

- **Reserva de estoque.** No Event Storming (16/10) ficou definido que o estoque é *reservado* quando o orçamento é
  gerado, *liberado* se o orçamento for reprovado e *baixado* na aprovação. A pergunta levantada na call foi: "quem me
  garante que nesse momento eu tenha estoque ainda?". Atualizar a OS e o estoque precisa acontecer de forma atômica
  (ver [ADR 003](0003-estrategia-de-transacoes.md)).
- **Unicidade.** CPF/CNPJ do cliente, placa do veículo e nome de serviços e peças não podem se repetir.
- **Valores monetários.** Preços de serviços e peças e o valor do orçamento precisam de precisão decimal exata.
- **Delete lógico.** Cadastros não são apagados, e uma OS só muda de status para *Cancelado* (decisão de 21/10).

Requisitos não funcionais relacionados ([Miro](https://miro.com/app/board/uXjVEYDydiQ=/?share_link_id=413287603523)):

- **RNF08:** operações transacionais de estoque e orçamento devem preservar a consistência mesmo em falhas ou
  requisições concorrentes. Não podem gerar saldo negativo, reservas parciais ou consumo/devolução duplicados.
- **RNF04:** quando a API ou o banco estiver indisponível, as operações afetadas devem responder com status 503 e
  mensagem explícita, sem gravar alterações parciais.

Restrições do projeto:

- O enunciado do Tech Challenge não exige um banco específico.
- Equipe pequena e prazo curto por entrega. A escolha deve ser algo que o time já conhece.
- A aplicação é um monólito com um único banco ([ADR 001](0001-monolito-em-camadas.md)) e roda localmente e na avaliação
  via Docker Compose.
- O projeto é incremental e será ampliado nas próximas fases.

Na reunião de 16/10, o André comentou que tem usado bastante o PostgreSQL. O Alexandre propôs usar uma solução open
source e lembrou que, em sistemas corporativos, é comum encontrar Oracle, que é pago. Ninguém trouxe outra opção, e o
time fechou com o PostgreSQL. Em 21/10, o André não conseguia subir a aplicação por causa do banco e testou com um
PostgreSQL em Docker, que funcionou "redondo". Isso levou ao `docker-compose.yml` (PR #72).

Na mesma reunião, o André reconheceu que o time ainda não tinha visto como avaliar trade-offs de banco. Por isso,
revisamos a escolha com os critérios abaixo.

### Critérios de escolha: CAP e PACELC

Escolher entre "SQL ou NoSQL" ou pelo formato dos dados é uma análise superficial. O banco deve ser escolhido pelos
atributos de que a aplicação precisa, classificados pelos teoremas CAP e PACELC:

- **CAP** (Eric Brewer, 2000): num sistema distribuído, diante de uma **partição de rede (P)**, é preciso escolher entre
  **consistência (C)** e **disponibilidade (A)**.
- **PACELC** (Daniel Abadi, 2012) estende o CAP: se há partição (**P**), escolhe-se entre **A** e **C**. Senão (**E**,
  de *else*), escolhe-se entre **latência (L)** e **consistência (C)**. Garantir que todas as réplicas tenham o dado
  antes de responder aumenta a latência. Responder antes leva à consistência eventual.

Aplicando ao nosso caso:

1. **Consistência é obrigatória (RNF08).** A reserva de estoque é parecida com o exemplo do saldo bancário: se uma
   leitura vier de uma réplica desatualizada, duas OS podem reservar a mesma peça. É o mesmo problema do "Pix infinito".
   Diferente de um like numa rede social, aqui um dado desatualizado gera erro de negócio. Precisamos de um banco
   **CP/EC**, com transações ACID.
2. **Não somos um sistema distribuído.** A ADR 001 define um monólito com um único banco, sem réplicas nem sharding. Não
   há partição entre nós do banco para tolerar, e não existe hoje requisito de alta disponibilidade 24x7, escala global
   ou escrita massiva. Os trade-offs de PACELC que justificam bancos distribuídos não se aplicam nesta fase.
3. **O domínio é relacional e estruturado.** Cadastros referenciados pela OS, unicidade e valores monetários pedem
   integridade referencial e constraints.

| Critério                                    | O que precisamos                                                       |
|---------------------------------------------|------------------------------------------------------------------------|
| Classificação PACELC                        | CP / EC (consistência sempre)                                          |
| Tipo de banco                               | Relacional, com ACID                                                   |
| Topologia                                   | Single node é suficiente (monólito, um banco)                          |
| Consistência ajustável em tempo de execução | Não é necessária: toda escrita relevante precisa de consistência forte |
| Casos de uso compatíveis                    | Sistema empresarial, com controle de estoque e muitos relacionamentos  |

## Decisão

Usamos o **PostgreSQL 18**, um banco relacional open source, como único banco da aplicação.

O PostgreSQL atende todos os critérios. Ele é um banco relacional single node com ACID, classificado como CP/EC. Seus
casos de uso típicos são "aplicações web, sistemas empresariais, comércio eletrônico, ERP e sistemas que precisam de
muita consistência, garantia, relacionamento e estruturação dos dados", que é o perfil da oficina. A principal limitação
dele é não ter sido feito para ambientes distribuídos: replicação baseada em WAL, sem consistência ajustável e sharding
manual. Essa limitação não nos afeta, porque rodamos um único nó. Em um único nó, ele entrega consistência forte com
baixa latência, sem ter que escolher entre as duas.

- **Execução:** container `postgres:18-alpine` no `docker-compose.yml`, com volume `pgdata` montado em
  `/var/lib/postgresql` (caminho esperado pela imagem a partir da versão 18) para persistir os dados e healthcheck
  `pg_isready`. A API só sobe depois que o banco estiver saudável.
- **Acesso:** Spring Data JPA (Hibernate) com o driver `org.postgresql:postgresql`. Todo o acesso ao banco fica em
  `infrastructure/persistence/<contexto>/` (entidade JPA, `*JpaRepository`, `*PersistenceMapper` e
  `*PersistenceAdapter`), atrás dos `PortOut` do `core`, como define a ADR 001.
- **Configuração:** URL, usuário e senha vêm de variáveis de ambiente (`DATASOURCE_HOST`, `DATASOURCE_USERNAME`,
  `DATASOURCE_PASSWORD`) no `application.yaml`. O compose define os valores de desenvolvimento.
- **Schema e migrations:** o schema é versionado com **Flyway** (`spring-boot-starter-flyway` e
  `flyway-database-postgresql`). Cada mudança é um arquivo `V<n>__descricao.sql` em `src/main/resources/db/migration`, e
  a API aplica as migrations pendentes na subida. Hoje existem a `V1__schema_inicial.sql` (tabelas das entidades atuais)
  e a `V2__perfis_iniciais.sql` (perfis `ADMINISTRATOR`, `MANAGER` e `OPERATOR`, exigidos pelo `/auth/register`). O
  Hibernate roda com `ddl-auto: validate`: ele não altera o banco, só confere na subida se as entidades batem com as
  tabelas, e a API não sobe se houver divergência. Uma migration já aplicada nunca é editada, porque o Flyway valida o
  checksum. Toda correção entra numa migration nova.
    - Em 21/10, o time tinha combinado gerar as tabelas pelo Hibernate (`ddl-auto`) nesta fase. A troca para o Flyway
      veio junto com o primeiro schema completo (PR #83), antes de existirem dados que precisassem ser preservados.

### Como o banco atende às regras

| Necessidade                        | Como é tratada                                                         | Exemplo no código                                                                                 |
|------------------------------------|------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| Relacionamentos                    | Chaves estrangeiras e tabelas de associação                            | `VeiculoEntity` → `ClienteEntity` (`@ManyToOne`); `servico_peca_insumo` (`@ManyToMany`)           |
| Unicidade                          | Constraints `UNIQUE` no banco, além da validação no caso de uso        | `documento` em `clientes`, `placa` em `veiculos`, `nome_normalizado` em `servico` e `peca_insumo` |
| Reserva de estoque                 | Transações ACID: OS e estoque são atualizados juntos ou nada é gravado | Colunas `quantidade` e `reservado` em `peca_insumo`                                               |
| Valores monetários                 | Tipo `numeric` com precisão fixa                                       | `preco` com `precision = 12, scale = 2`                                                           |
| Delete lógico                      | Flag de ativo ou status, sem `DELETE` físico                           | `ativo` em `ClienteEntity` e `VeiculoEntity`                                                      |
| Enums legíveis                     | Enum gravado como texto                                                | `tipo_documento` com `@Enumerated(EnumType.STRING)`                                               |
| Schema igual em todos os ambientes | Migrations versionadas, conferidas pelo Hibernate na subida            | `V1__schema_inicial.sql` e `ddl-auto: validate`                                                   |
| Dados obrigatórios                 | Migration de dados                                                     | Perfis em `V2__perfis_iniciais.sql`                                                               |

## Alternativas consideradas

As alternativas estão agrupadas pela classificação PACELC e pelo tipo de banco.

| Banco                                                      | PACELC   | Tipo                                  | Situação                                              |
|------------------------------------------------------------|----------|---------------------------------------|-------------------------------------------------------|
| **PostgreSQL**                                             | CP / EC  | Relacional single node (ACID)         | **Escolhido**                                         |
| MySQL / MariaDB                                            | CP / EC  | Relacional single node (ACID)         | Viável, sem vantagem                                  |
| Oracle                                                     | CP / EC  | Relacional (ACID)                     | Descartado: licença paga                              |
| CockroachDB, YugabyteDB                                    | CP / EC  | Relacional distribuído (ACID)         | Descartado por enquanto: complexidade sem necessidade |
| Spanner (Google)                                           | CP / EC  | Relacional distribuído (ACID)         | Descartado: custo e dependência de nuvem              |
| MongoDB                                                    | CP / EC  | Documentos (ACID)                     | Descartado: domínio relacional                        |
| Cassandra, ScyllaDB, DynamoDB, Riak                        | AP / EL  | Colunar / chave-valor                 | Descartado: consistência eventual                     |
| Redis, Etcd, FoundationDB, Neo4j, Elasticsearch, Couchbase | variados | Chave-valor, grafo, busca, documentos | Fora do caso de uso                                   |
| H2                                                         | —        | Relacional em memória                 | Descartado: não persiste dados                        |

### 1. Outros relacionais single node: MySQL / MariaDB e Oracle

- **Prós:** mesma classificação CP/EC e o mesmo modelo relacional com ACID. O Oracle é muito comum em sistemas
  corporativos.
- **Contras:** o MySQL atenderia o projeto, mas não traz vantagem sobre o PostgreSQL para este domínio. O Oracle tem
  licença paga, imagem Docker pesada e configuração mais trabalhosa para rodar localmente.
- **Por que não:** o time tem mais familiaridade com o PostgreSQL e preferiu uma solução open source (16/10). O projeto
  não precisa de nada exclusivo do Oracle.

### 2. Relacionais distribuídos: CockroachDB, YugabyteDB e Spanner

- **Prós:** são CP/EC e ACID como o PostgreSQL, e também escalam horizontalmente com tolerância a partição (Raft no
  CockroachDB e no YugabyteDB, Paxos e TrueTime no Spanner). O CockroachDB e o YugabyteDB são compatíveis com o
  protocolo do PostgreSQL.
- **Contras:** exigem um cluster de vários nós. Cada escrita precisa do acordo da maioria das réplicas, então a
  consistência custa latência (o "EC" do PACELC). A operação e os testes ficam mais complexos. O Spanner só existe no
  Google Cloud e é caro.
- **Por que não:** os casos de uso desses bancos são inventário global, fintech multirregião e SLA de 99,999%. Não temos
  esses requisitos. Ficam como **caminho de evolução**: se um dia o volume exigir distribuição, o CockroachDB ou o
  YugabyteDB aproveitam boa parte do SQL e do driver do PostgreSQL.

### 3. Documentos com consistência: MongoDB

- **Prós:** também é CP/EC e tem transações ACID. O esquema é flexível, e a OS poderia ser gravada como um único
  documento.
- **Contras:** o domínio é relacional. Cliente, veículo, serviço e peça são cadastros próprios, *referenciados* pela OS
  e não copiados para dentro dela. Sem chaves estrangeiras, a integridade ficaria toda no código, e a reserva de estoque
  envolveria atualizar a OS e várias peças ao mesmo tempo.
- **Por que não:** a consistência ele entregaria, mas perderíamos integridade referencial e constraints, que o domínio
  também precisa.

### 4. Bancos AP/EL: Cassandra, ScyllaDB, DynamoDB e Riak

- **Prós:** alta disponibilidade, baixa latência e escrita massiva. Alguns permitem ajustar a consistência por operação
  (quórum no Cassandra, *consistent read* no DynamoDB).
- **Contras:** por padrão priorizam disponibilidade e aceitam consistência eventual, o oposto do que a reserva de
  estoque precisa. Forçar consistência forte neles (ex.: quórum em toda operação) vai contra a natureza do banco e
  aumenta a latência. Também não têm joins nem chaves estrangeiras.
- **Por que não:** os casos de uso deles são redes sociais, IoT, séries temporais e telemetria, muito diferentes da
  oficina. Vale a regra de escolher um banco que já foi projetado para o que se quer fazer, em vez de modificar a
  essência dele.

### 5. Bancos especializados: Redis, Etcd, FoundationDB, Neo4j, Elasticsearch e Couchbase

- **Por que não:** atendem outros problemas, como cache e sessão (Redis), configuração de cluster (Etcd), metadados em
  grande escala (FoundationDB), grafos e detecção de fraude (Neo4j) e busca full-text (Elasticsearch). Podem entrar no
  futuro como **complemento**, por exemplo um cache em Redis, mas não como banco principal.

### 6. H2 em memória como banco da aplicação

- **Prós:** nada para instalar, sobe junto com a aplicação.
- **Contras:** os dados se perdem a cada reinício, e o comportamento difere do PostgreSQL (tipos, SQL, constraints).
- **Por que não:** a avaliação roda via Docker Compose, e subir um PostgreSQL ali é simples. Os testes do `core` já não
  dependem de banco, porque usam mocks dos ports.

### 7. Como versionar o schema: `ddl-auto`, Flyway ou Liquibase

| Opção                                         | Situação                              |
|-----------------------------------------------|---------------------------------------|
| `ddl-auto` do Hibernate (`update` / `create`) | Usado no início. Substituído          |
| **Flyway**                                    | **Escolhido**                         |
| Liquibase                                     | Viável, mais pesado do que precisamos |

- **`ddl-auto`**
    - **Prós:** nenhum arquivo para manter, porque as tabelas saem das entidades.
    - **Contras:** não guarda histórico, não remove colunas nem renomeia tabelas, não cria dados iniciais (como os
      perfis) e pode deixar bancos diferentes entre as máquinas do time e a avaliação. O schema real depende da ordem em
      que cada um subiu a aplicação.
    - **Por que não:** foi aceitável enquanto o banco era recriado a toda hora. Com o schema completo, os perfis
      obrigatórios e a avaliação via Docker Compose, precisamos de um banco reproduzível.
- **Flyway**
    - **Prós:** as migrations são SQL puro, que o time já sabe ler e escrever, e ficam no repositório junto com o código
      que as exige. A tabela `flyway_schema_history` registra o que foi aplicado, e o checksum impede alterar o
      histórico. O Spring Boot tem integração nativa: basta a dependência, sem configuração extra.
    - **Contras:** o SQL é específico do PostgreSQL, e trocar de banco exigiria reescrever as migrations. Desfazer uma
      migration exige outra migration, porque não há *undo* na versão gratuita.
    - **Por que sim:** é a opção mais simples que resolve o problema e a mais comum em projetos Spring.
- **Liquibase**
    - **Prós:** changesets em XML, YAML ou SQL, independência de banco e rollback declarado.
    - **Contras:** mais conceitos (changelog, changeset, contexts) e mais configuração. Não precisamos ser independentes
      de banco (a escolha do PostgreSQL é esta ADR), e o rollback pode ser feito com uma migration nova.
    - **Por que não:** a complexidade a mais não se paga num monólito com um único banco.

## Consequências

### Positivas

- Integridade garantida pelo próprio banco (chaves estrangeiras, `UNIQUE`, `NOT NULL`), e não só pelo código.
- Transações ACID para a reserva e a baixa de estoque, sem consistência distribuída.
- Open source, sem custo de licença, e com imagem oficial leve (`alpine`).
- Tecnologia que o time já conhece, o que reduz o tempo de setup e de debug.
- Bom suporte no ecossistema Spring (Spring Data JPA, `spring-boot-starter-data-jpa-test`) e em ferramentas de teste
  como Testcontainers.
- Como o acesso fica atrás dos `PortOut`, trocar ou evoluir o banco afeta só `infrastructure`.
- Schema versionado e reproduzível: qualquer máquina (ou a avaliação) recria o banco do zero com `docker compose up`,
  com as mesmas tabelas e perfis. Uma entidade que diverge do schema é detectada na subida, e não em produção.

### Negativas e trade-offs

- O PostgreSQL é single node e não foi feito para ambientes distribuídos. Se for preciso escalar, o caminho natural são
  réplicas de leitura (replicação via WAL). Elas são assíncronas por padrão e trazem consistência eventual: leituras
  ligadas ao estoque e ao status da OS teriam que continuar indo para o nó principal. Sharding no PostgreSQL é manual e
  trabalhoso. Se a escala exigir distribuição de verdade, esta ADR deve ser revista, com um relacional distribuído como
  candidato.
- Rodar a aplicação localmente exige subir também o container do banco (`docker compose up`).
- Toda mudança de entidade precisa vir com uma migration nova. Esquecer a migration faz a API não subir, por causa do
  `ddl-auto: validate`. O erro aparece cedo, mas exige disciplina no PR.
- Volumes criados antes das migrations (pelo `ddl-auto`) já têm tabelas sem histórico do Flyway, e a API não sobe (
  "Found non-empty schema (s) ... without schema history table"). Em desenvolvimento, a saída é recriar o volume com
  `docker compose down -v`, o que apaga os dados locais.
- As migrations usam SQL do PostgreSQL. Se esta ADR for revista para outro banco, elas precisam ser reescritas ou
  adaptadas (os relacionais distribuídos compatíveis com PostgreSQL reduzem esse custo).
- Os perfis já são criados pela `V2__perfis_iniciais.sql`. Ainda faltam as permissões de cada perfil, o usuário
  administrador e as credenciais de demonstração, que precisam ser definidos para a avaliação. Credenciais não devem ir
  para uma migration versionada no repositório.
- As credenciais do compose são de desenvolvimento e ficam em texto no repositório. Não devem ser usadas em outro
  ambiente.

## Referências

- [ADR 001 — Monólito em camadas (ports & adapters)](0001-monolito-em-camadas.md)
- [ADR 003 — Estratégia de transações](0003-estrategia-de-transacoes.md): reserva de estoque na geração do orçamento e
  liberação quando o orçamento é reprovado.
- Event Storming do grupo (Miro) e a reunião de 16/10/2026
- [Requisitos não funcionais (Miro)](https://miro.com/app/board/uXjVEYDydiQ=/?share_link_id=413287603523)
- Decisões registradas no grupo do time entre 16/10 e 23/10/2026
- [Documentação do PostgreSQL 18](https://www.postgresql.org/docs/18/)
- [Documentação do Flyway](https://documentation.red-gate.com/flyway)
  e [Spring Boot — Database initialization with Flyway](https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
- PR #83: adoção do Flyway com schema inicial e perfis
- Renato Augusto, vídeo sobre como escolher o banco de dados (teoremas CAP e PACELC) e a planilha de classificação de
  bancos que o acompanha
- Eric Brewer, *Towards Robust Distributed Systems* (PODC 2000): teorema CAP
- Daniel Abadi, *Consistency Tradeoffs in Modern Distributed Database System Design* (IEEE Computer, 2012): PACELC
